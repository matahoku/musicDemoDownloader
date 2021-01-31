package musicDemoDownloader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;

/** *
 *  検索したアーティスト名をWikipediaで調べ、関連しそうな言葉を抽出して表示します。
 * 例：宇多田ヒカル => 「宇多田ヒカルといえば『日本』ですね」
 *
 * Wikipediaのテキストを形態素解析し、一番多く出現した固有名詞を星印で表示します。
 * Wikipediaにページがない場合は何も出力しません。
 * 固有名詞の出現数が同じ場合、ランダムで最大３語抽出して表示します。
 * 形態素解析は外部ライブラリを使用する。『 Kuromoji 』
 * WikipediaのAPI 『 MediaWiki 』
 *
 *  使用方法：コンストラクタにアーティスト名を渡し、wikiPageGetAndGetWord()で最頻出単語を取得
 *
 * @author matahoku
 */
public class Wiki {

	//WikipediaへgetRequest用のURL(未完成)    titlesのパラメータを取得し完全なURLを作成する
	private String UnfinishedUrl = "https://ja.wikipedia.org/w/api.php?format=xml&action=query&prop=extracts&explaintext&redirects=1&titles=%s";
	//WikipediaへのgetRequest用の完全なURL
	private String completeUrl;
	//検索をかけるアーティスト名 urlパラメータ -> titles値
	private String artisteName;
	//解析結果 単語：出現数
	Map<String, Integer> analysisResultMap ;
	//最頻出単語を格納(同数であった場合全て)
	List<String> maxWordList;
	//最頻出単語の出現数
	private int maxCount ;
	//xml記号一覧
	private String xmlArray[] = {"xml","version","api","batchcomplete","query","pages","page _idx"
			                      ,"pageid","ns","title","extract","space","preserve","page"};

	/**
	 *	アーティスト名をセットし、完全なURLを作成します。
	 *
	 * @param artisteName アーティスト名
	 */
	public Wiki(String artisteName) {
		this.artisteName = artisteName;
		completeUrl = String.format(UnfinishedUrl , artisteName);
	}

	/**
	 *	入力されたアーティスト名を元にWikipediaページを取得します。
	 *
	 * @param artisteName アーティスト名
	 */
	public void wikiPageGetAndGetWord() {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(this.completeUrl)).build();
			BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
			HttpResponse<String> response = HttpClient.newBuilder().build().send(request, bodyHandler);

			//形態素解析にかけ、頻出単語を取得する。
			if(response.body().contains("missing") || response.body().contains("invalid") || !response.body().contains("query")) {
				//存在しないページ・無効なページにアクセスした場合は何もしない
			}else {
				//記事が存在する場合は解析に移行する
				this.frequentWord(response.body());
				//System.out.println(response.body());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wikipediaのテキストを形態素解析し、一番多く出現した固有名詞で文章を作成し表示します。
	 * 例：宇多田ヒカル => 「宇多田ヒカルといえば『アメリカ』ですね」
	 *
	 * Wikipediaにページがない場合は何も出力しません。
     * 固有名詞の出現数が同じ場合、ランダムで最大３語抽出して表示します
	 *
	 * 形態素解析(Kuromoji)を使用
	 *
	 * @param responseBody wikipediaの記事
	 *
	 * @return maxWord 最頻出の単語
	 */
	public void frequentWord(String responseBody) {
		//kuromojiの使用準備
		Tokenizer tokenizer = Tokenizer.builder().build();
		//取得したwikipediaページを解析
		List<Token> tokens = tokenizer.tokenize(responseBody);
		 //key：単語　value：出現頻度
		this.analysisResultMap= new HashMap<String, Integer>();

		//単語のみ抽出
        for (Token token : tokens) {
        	//全ての情報を取得   features[0]から名詞を取得
            String[] features = token.getAllFeaturesArray();
           //固有名詞のみmapに追加する
            if(features[0].equals("名詞") && features[1].equals("固有名詞") ){
            	  if(analysisResultMap.containsKey(token.getSurfaceForm())) {
                  	//すでに出現した単語であったら出現回数を+1する
                  	Integer num = analysisResultMap.get(token.getSurfaceForm()) + 1;
                  	analysisResultMap.replace(token.getSurfaceForm(), num);
                  }else {
                  	 //xml属性名・アーティスト名以外を抽出する
                  	 if(!this.removeAttributeName(token, this.xmlArray,this.artisteName)) {
                  		//初めて出現する単語であったら出現回数１をセットする。
                  		analysisResultMap.put(token.getSurfaceForm(), 1);
                  	 }
                  }
            }
        }

        //最も出現頻度が高い単語を集めたリスト(出現数が同数の場合に対応)
        this.maxWordList = new ArrayList<String>();
        //最も出現頻度が高い単語の出現数
        this.maxCount = 0;

        //最も多い出現数を取得
        for(Map.Entry<String, Integer> entry : analysisResultMap.entrySet()) {
        	  if(entry.getValue() > maxCount) {
        		  maxCount = entry.getValue() ;
        	  }
        }

        //最も出現頻度が高い単語をリストに格納
        for(Map.Entry<String, Integer> entry : analysisResultMap.entrySet()) {
        	if(entry.getValue() == maxCount) {
        		maxWordList.add(entry.getKey());
        	}
        }

        //完成した文章を表示する
        if(maxWordList.size() > 1) {
        	//最大出現数と一致する単語が複数存在する場合
        	if(maxWordList.size() < 4) {
        		//最大出現数と一致する単語が４未満の場合
        		for(String str : maxWordList) {
        			System.out.println(String.format("☆%sといえば『%s』ですね",artisteName, str));
        		}
        	}else {
        		//最大出現数と一致する単語が４つ以上の場合
        		//リストの中身をシャッフルし、先頭から３つ取得する
        		Collections.shuffle(maxWordList);
        		for(int i = 1; i <= 3; i++) {
        			System.out.println(String.format("☆%sといえば『%s』ですね",artisteName,maxWordList.get(i)));
        		}
        	}
        }else if(maxWordList.size() == 1){
        	//最大出現数と一致する単語が１つのみの場合
        	System.out.println(String.format("☆%sといえば『%s』ですね",artisteName,maxWordList.get(0)));
        }else {
        	//要素数０ 解析がうまくいっていない
        	System.out.println("※解析で不具合が起きています。確認してください。");
        }
	}

	/**
	 *解析結果の確認用
	 *
	 * @param map wikipediaの解析結果
	 */
	public void analysisResult(Map<String, Integer>analysisResultMap , List<String> maxWordList,int maxCount) {
		 //map確認用
        System.out.println("■固有名詞での解析結果");
        for(Map.Entry<String, Integer> entry : analysisResultMap.entrySet()) {
            System.out.print(entry.getKey());
            System.out.print(":");
            System.out.println(entry.getValue());
        }

            System.out.println("--------------------");
            System.out.print("頻出の単語：");
            System.out.println(maxWordList);
            System.out.print("出現数：");
            System.out.println(maxCount);
            System.out.print("\n");
	}

	/**
	 * 名詞がxml属性名であるか確認します。
	 *
	 * @param map wikipediaの解析結果
	 *
	 * @return hasXml true:xmlの属性名である false:xmlの属性名ではない
	 */
	public boolean removeAttributeName(Token token, String xmlArray[],String artisteName) {
		//xml属性名または、検索したアーティスト名自身だったらyes
		boolean xmlOrArtiste = Arrays.asList(xmlArray).contains(token.getSurfaceForm())
				 || artisteName.contains(token.getSurfaceForm());

		return xmlOrArtiste;
	}

	/**
	 *解析結果の確認用です。
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		//確認用
		Wiki wiki = new Wiki("宇多田ヒカル");
		System.out.println(String.format("『%s』の検索結果", wiki.artisteName));
		System.out.print("\n");

		wiki.wikiPageGetAndGetWord();
		System.out.print("\n");

		//解析結果の確認用
		if(wiki.analysisResultMap != null && wiki.maxWordList != null && wiki.maxCount > 0) {
			wiki.analysisResult(wiki.analysisResultMap,wiki.maxWordList,wiki.maxCount);
		}
        System.out.println("※何も表示されない場合はwikipediaにページが存在しなかった場合です。");
       	System.out.println("※カタカナとひらがなの違いでも検索できなくなるので確認してください。");
	}
}
