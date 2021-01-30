# MusicDemoDownloader
Javaの標準入力でアーティスト名を入力すると、iTunes Storeでそのアーティスト名をキーワードとして検索し、デモ音源ファイルを指定場所にダウンロードします。<br>
また、検索したアーティスト名をWikipediaで調べ、関連しそうな言葉を抽出して表示します。
例：宇多田ヒカル => 「宇多田ヒカルといえば『日本』ですね」

## 使用クラス
■クラス MusicDemo
iTunes Storeでアーティスト名をキーワードとして検索し、デモ音源ファイルをダウンロードします。
 複数件ヒットした場合、最大10件の中からランダムで1曲ダウンロードします。
 見つからなかった場合、その旨をメッセージ出力します。
 映画やミュージックビデオは検索対象外とし、音楽のみダウンロードします。
 ファイルの保存場所はどこでもOKです。
 通信やJSON解析に以下の外部ライブラリを使用しています。
 「gson-2.8.6.jar」「comons-io-2.8.0.jar」

■クラス Wiki
引数で受け取った文字列(アーティスト名)をWikipediaで調べ、関連しそうな言葉を抽出して表示します。
  例：宇多田ヒカル => 「宇多田ヒカルといえば『日本』ですね」
  Wikipediaのテキストを形態素解析し、一番多く出現した固有名詞を星印で表示します。
  Wikipediaにページがない場合は何も出力しません。
  固有名詞の出現数が同じ場合、ランダムで最大３語抽出して表示します。
  このクラスを単体で使用した場合、固有名詞で分解した結果を全て表示します。
  形態素解析は以下の外部ライブラリを使用しています。
  「Kuromoji-0.7.7.jar」
  