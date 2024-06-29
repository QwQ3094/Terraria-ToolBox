package silkways.terraria.toolbox.fragment.toolbox

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.unity3d.player.UnityPlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import silkways.terraria.toolbox.R
import silkways.terraria.toolbox.databinding.ToolboxFragmentTerminalBinding
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class TerminalFragment: Fragment() {

    private var _binding: ToolboxFragmentTerminalBinding? = null
    private val binding get() = _binding!!

    private var linesCount = 0
    private val maxLines = 109

    private val commandHistory = mutableListOf<String>()
    private var historyPosition = -1

    private val commandExecutors = mutableMapOf<String, () -> Unit>()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.title = getString(R.string.terminal)

        _binding = ToolboxFragmentTerminalBinding.inflate(inflater, container, false)

        initializeCommands()

        setupUI()

        return binding.root
    }



    private fun setupUI() {
        with(binding.codeEditor) {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateLineNumberDisplay()
                }
            })
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    // 确保在按下回车键或完成动作时尝试执行命令
                    val command = text.toString().trim() // 获取并清理命令字符串
                    if (command.isNotBlank()) {
                        executeCommand(command)
                        appendNewLine()
                        moveCursorToEnd()
                    }
                    return@setOnEditorActionListener true
                }
                false
            }
        }
        binding.codeEditor.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                // 获取倒数第二行的命令文本
                binding.codeEditor.append("\n")
                val command = getLastSecondLineCommand()
                if (command.isNotBlank()) {
                    executeCommand(command)
                    appendNewLine()
                    moveCursorToEnd()
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun updateLineNumberDisplay() {
        val currentText = binding.codeEditor.text.toString()
        val lines = currentText.split("\n").take(maxLines) // 确保只计算最多maxLines行
        val formattedLineNumbers = List(lines.size) { index -> (index + 1).toString().padStart(4, ' ') }.joinToString("\n")
        binding.lineNumberContainer.text = formattedLineNumbers
    }

    private fun getLastSecondLineCommand(): String {
        val lines = binding.codeEditor.text.toString().split("\n")
        return if (lines.size > 1) lines[lines.size - 2].trim() else "" // 确保至少有两行才能获取倒数第二行
    }

    // 执行命令逻辑
    private fun executeCommand(command: String) {
        commandHistory.add(command)
        historyPosition = commandHistory.size

        // 确保在添加新行之前检查是否达到最大行数
        if (linesCount + 2 > maxLines) {
            clearScreen() // 清空屏幕内容
        } else {
            linesCount += 2
        }

        val executor = commandExecutors[command] ?: commandExecutors.entries.find { it.key.equals(command, ignoreCase = true) }?.value
        val output = executor?.invoke()?.toString() ?: "Unrecognized command: $command"
        binding.codeEditor.append("$output \n")
        updateLineNumberDisplay() // 更新行号显示
    }





    private fun appendNewLine() {
        binding.codeEditor.append("\n")
        linesCount++
    }

    private fun moveCursorToEnd() {
        binding.codeEditor.setSelection(binding.codeEditor.text.length)
    }







    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                      命令执行逻辑
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    private fun initializeCommands() {
        commandExecutors["help"] = { printHelp() }
        commandExecutors["clear"] = { clearScreen() }
        commandExecutors["start"] = { startGame() }
        commandExecutors["exit"] = { requireActivity().finish() }
    }




    @SuppressLint("SetTextI18n")
    private fun clearScreen() {
        binding.codeEditor.setText("") // 清空屏幕内容
        linesCount = 0 // 重置行数计数
    }

    private fun printHelp() {
        val helpMessage = """
            Available commands:
            help - Display this help message.
            clear - Clear the terminal screen.
            game - Start the game.
            exit - Exit the application.
        """.trimIndent()
        binding.codeEditor.append(helpMessage)
    }

    private fun startGame() {
        val intent = Intent(requireContext(), UnityPlayerActivity::class.java)
        startActivity(intent)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
