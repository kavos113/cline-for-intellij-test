package com.github.kavos113.clinetest.ui

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kavos113.clinetest.shared.ApiTokenInfo
import com.github.kavos113.clinetest.shared.message.ClineAsk
import com.github.kavos113.clinetest.shared.message.ClineAskOrSay
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.github.kavos113.clinetest.shared.message.ClineSay
import com.github.kavos113.clinetest.shared.message.ClineSayTool
import com.github.kavos113.clinetest.shared.message.ClineSayTools

val sampleMessages: List<ClineMessage> = listOf(
  ClineMessage(
    ts = System.currentTimeMillis() - 4500,
    type = ClineAskOrSay.Say,
    say = ClineSay.ApiReqStarted,
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 4000,
    type = ClineAskOrSay.Say,
    say = ClineSay.ApiReqFinished,
    text = jacksonObjectMapper().writeValueAsString(
      ApiTokenInfo(
        tokensIn = 100,
        tokensOut = 50,
        cost = 0.24
      )
    )
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 3900,
    type = ClineAskOrSay.Say,
    say = ClineSay.Text,
    text = """
# Markdown Example
## Sample Code
```kotlin
fun main() {
    println("Hello, World!")
}
```

## Sample Text
This is a sample text.
- Item 1
- Item 2
- Item 3
        """
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 3500,
    type = ClineAskOrSay.Ask,
    ask = ClineAsk.Command,
    text = "git status"
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 3000,
    type = ClineAskOrSay.Say,
    say = ClineSay.CommandOutput,
    text = "On branch main"
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 2900,
    type = ClineAskOrSay.Say,
    say = ClineSay.CommandOutput,
    text = "Your branch is up to date with 'origin/main'."
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 2800,
    type = ClineAskOrSay.Say,
    say = ClineSay.CommandOutput,
    text = ""
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 2700,
    type = ClineAskOrSay.Say,
    say = ClineSay.CommandOutput,
    text = "nothing to commit, working tree clean"
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 2500,
    type = ClineAskOrSay.Ask,
    ask = ClineAsk.Followup,
    text = "追加情報を求めています"
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 2000,
    type = ClineAskOrSay.Say,
    say = ClineSay.Text,
    text = "追加情報が必要な場合は指定してください"
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 1500,
    type = ClineAskOrSay.Ask,
    ask = ClineAsk.Tool,
    text = jacksonObjectMapper().writeValueAsString(
      ClineSayTool(
        tool = ClineSayTools.EditedExistingFile,
        path = "./src/main.kt",
        diff = """
                    - print("Hello, World!")
                    + println("Hello, World!")
                """.trimIndent()
      )
    )
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 1500,
    type = ClineAskOrSay.Ask,
    ask = ClineAsk.Tool,
    text = jacksonObjectMapper().writeValueAsString(
      ClineSayTool(
        tool = ClineSayTools.NewFileCreated,
        path = "./src/hello.java",
        content = """
                    class HelloWorld {
                        public static void main(String[] args) {
                            System.out.println("Hello, world!");
                        }
                        
                        private String name;
                        public HelloWorld(String name) {
                            this.name = name;
                        }
                    }
                """.trimIndent()
      )
    )
  ),
  ClineMessage(
    ts = System.currentTimeMillis(),
    type = ClineAskOrSay.Say,
    say = ClineSay.UserFeedback,
    text = "フィードバックありがとうございます"
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 1000,
    type = ClineAskOrSay.Say,
    say = ClineSay.Error,
    text = "エラーが発生しました: ファイルが見つかりません"
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 4500,
    type = ClineAskOrSay.Say,
    say = ClineSay.ApiReqStarted,
  ),
  ClineMessage(
    ts = System.currentTimeMillis() - 500,
    type = ClineAskOrSay.Ask,
    ask = ClineAsk.ApiReqFailed,
    text = "APIリクエストに失敗しました"
  ),
)