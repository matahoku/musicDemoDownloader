package musicDemoDownloader;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import com.google.gson.Gson;

/**
 * iTunes Storeでアーティスト名をキーワードとして検索し、デモ音源ファイルをダウンロードします。
 * 複数件ヒットした場合、最大10件の中からランダムで1曲ダウンロードします。
 * 見つからなかった場合、その旨をメッセージ出力します。
 * 映画やミュージックビデオは検索対象外とし、音楽のみダウンロードします。
 * ファイルの保存場所はどこでもOKです。
 * 通信やJSON解析に以下の外部ライブラリを使用しています。
 * 「gson-2.8.6.jar」「comons-io-2.8.0.jar」
 *
 * @author matahoku
 */

public class MusicDemo {

	//iTunes Search API へのアクセス  形式：https://itunes.apple.com/search?parameterkeyvalue
	//パラメータtermにアーティスト名を当てはめる
	//完全なURLは、completedURL = URL1 + artisteName + URL2 となる。
	private String url1 = "https://itunes.apple.com/search?term=";
	private String url2 = "&country=JP&media=music&attribute=artistTerm&limit=2&lang=ja_jp";
	private String completedURL = null;
	// アーティスト名
	private String artisteName;
	//ダウンロードする楽曲名
	private String collectionName;
	//試曲デモのURL
	private String demoUrl = null;
	//試曲を保存する場所のパス
	private String storageLocation;

	/**
     *  試曲の保存場所のパスを設定します。最後に区切り文字は不要です。
     *  (OSX 例)"/○○○/○○○/Desktop"
     *
     * @param storageLocation　試曲を保存する場所のパス
     */
		public MusicDemo(String storageLocation) {
			this.storageLocation = storageLocation;
		}

	/**
     * iTunes Search API に GETrequest を行います。
     * GETrequest例：http://itunes.apple.com/search?term=aiko&country=JP&lang=ja_jp&media=music&entity=song&attribute=artistTerm&limit=10
     *
     * ■必須パラメータ
     * term: 検索キーワード アーティスト名が入る
     * country: 検索対象となる国の2桁コード 日本:jp 英語:en
     *
     * ■任意パラメータ
     * media: 取得対象の商品の種類 music
     * entity: mediaで指定した取得対象の商品の中で、さらに細かく指定する値。人名で音楽を検索。 artistTerm
     * attribute: 商品をどの属性を、検索キーワードの対象とするのかを指定する。
     * limit: 取得件数を指定する。10
     * lang: 言語を指定する。日本 ja_jp
     *
     */
	public void getRequest(String completedURL) {
		try {
			//HTTPリクエストの送信内容を設定
			HttpRequest request = HttpRequest.newBuilder(URI.create(this.completedURL)).build();
			//HTTPリクエストのボディを作成
			BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
			//HTTPレスポンスの内容が集約されたクラスを作成
			HttpResponse<String> response = HttpClient.newBuilder().build().send(request,bodyHandler);
			//レスポンスの取得
			String responseBody = response.body();
			//JSON解析
			this.useJSON(responseBody);
			//BodyHandlers.ofFileでレスポンスをファイルとして保存することが出来る。

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * 入力されたアーティスト名を使用し、完全なURLを作成します。
     *
     * @param artisteName 入力されたアーティスト名
     *
     */
	public void combineURL(String artisteName) {
		if(artisteName == null) {
			throw new NullPointerException();
		}

		//URLを連結
		StringBuilder sb = new StringBuilder();
		sb.append(url1).append(artisteName).append(url2);

		//文字列に変換
    	this.completedURL = sb.toString();
	}

	/**
     * iTunes Search API から返されたJSONデータを解析し,デモ音源URL,ヒット件数を取得します。
     *
     * 楽曲のヒット数は最大10件です。
     * 複数件ヒットした場合、ランダムで1曲デモ音源をダウンロードします。
     * 見つからなかった場合、その旨をメッセージ出力します。
     *
     * JSONサンプル： { "resultCount":1, "results": [{"wrapperType":"track", "kind":"song"}] }
     *
     * @param responseBody レスポンス本文
     *
     */
	public void useJSON(String responseBody) {
		Gson gson = new Gson();

	   //JSON文字列を解析しdemoURLを取得する。
	   //デバッグ用あとでなおすこと
	   Artiste artiste = gson.fromJson(responseBody, Artiste.class);

	   System.out.print("\n");
	   System.out.println("■検索結果");
	   System.out.println("ヒット数： " + artiste.resultCount);
	   System.out.print("\n");

	   //ヒット件数による分岐
	   Data data = null;
	   if(artiste.resultCount == 0) {
		   //ヒット数：０
		   System.out.println("※楽曲が見つからないため、デモ音源をダウンロードすることが出来ません。");
	   }else if(artiste.resultCount == 1) {
		   //ヒット数：１
		   data = artiste.results.get(0);
		   System.out.println("１曲ヒットしました。デモ音源をダウンロードします。");
	   }else {
		   //ヒット数：２件以上１０件以下
		   data = this.randomSong(artiste);
		   System.out.println("※楽曲が複数件ヒットしたため、ランダムに１曲デモ音源をダウンロードします。");
	   }

	   //楽曲名、デモ音源のURLを取得します。
	   if(artiste.resultCount != 0) {
		   this.collectionName = data.getCcollectionName();
		   this.demoUrl = data.getPreviewUrl();
		   System.out.println("デモURL:" + demoUrl);
	   }else {
		   //ヒットしなかったため何もしない
	   }
	}


	/**
     * 試曲デモのURLを使用し、デモ音源をダウンロードします。(commons IO)
     *
     * @param demoUrl 試曲デモのURL
     *
     */
	public void downloadDemo(String demoUrl) {
		try {
			//nullチェック
			if(demoUrl == null) {
				return;
			}
			//取得するファイル名
			String toFileName = String.format("%s/%s_%s_demo.m4a",this.storageLocation, this.artisteName ,this.collectionName);

			//ダウンロード
			URL url = new URL(demoUrl);
			File toFile = new File(toFileName);
			FileUtils.copyURLToFile(url, toFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
     * 楽曲が複数件ヒットした場合、その中からランダムに1曲取得します。
     *
     * @param artiste JSONを解析し取得したアーティスト情報
     *
     * @return 試曲デモのURL
     *
     */
	public Data randomSong(Artiste artiste) {
		//ヒットした曲数を取得する
		int size = artiste.resultCount;
		//ランダムに選ばれた楽曲の要素番号を取得する
		Random rondom = new Random();
		int songNum = rondom.nextInt(size);

		Data data = artiste.results.get(songNum);
		return data;
	}

	/**
     * アプリのメイン処理です。
     * @param args
     */
	public static void main(String[] args) {

		//MusicDemoのインスタンス生成時、引数に試曲デモの保存場所(path)を指定する
		MusicDemo musicApp = new MusicDemo("/○○○/○○○/Desktop");

		//アーティスト名を入力しURLを完成させる
		System.out.println("■検索するアーティスト名を入力してください");
		Scanner sc = new Scanner(System.in);
		musicApp.artisteName = sc.nextLine();
		musicApp.combineURL(musicApp.artisteName);
		sc.close();

		//完成したURLをiTunes Search APIに送信
		musicApp.getRequest(musicApp.completedURL);
		musicApp.downloadDemo(musicApp.demoUrl);

		//Wikipediaから関連する単語を取得
		System.out.print("\n");
		Wiki wiki = new Wiki(musicApp.artisteName);
		wiki.wikiPageGetAndGetWord();

	}
}

/**
 *JSON解析用クラス
 */
class Artiste {
	//レスポンスフィールド
	//大カテゴリ
	public int resultCount;
	public List<Data> results;

	/**
     * JSONを解析しJavaのインスタンスを作成します。
     *
     * @param resultCount JSONパラメータ
     * @param results JSONパラメータ
     */
	public Artiste(int resultCount, List<Data> results) {
		this.resultCount = resultCount;
		this.results = results;
	}

}

/**
 *JSON解析用クラス results内のデータを格納
 *
 */
class Data{
	//resultsカテゴリ
//	public String wrapperType;
//	public String kind;
//	public String artistId;
//	public String collectionId;
//	public String trackId;
//	public String artistName;
	public String collectionName;
//	public String trackName;
//	public String collectionCensoredName;
//	public String trackCensoredName;
//	public String artistViewUrl;
//	public String collectionViewUrl;
//	public String trackViewUrl;
	public String previewUrl;
//	public String artworkUrl30;
//	public String artworkUrl60;
//	public String artworkUrl100;
//	public String collectionPrice;
//	public String trackPrice;
//	public String releaseDate;
//	public String collectionExplicitness;
//	public String trackExplicitness;
//	public String discCount;
//	public String discNumber;
//	public String trackCount;
//	public String trackNumber;
//	public String trackTimeMillis;
//	public String country;
//	public String currency;
//	public String primaryGenreName;
//	public String isStreamable;

	public Data(String collectionName ,String previewUrl) {
		this.collectionName = collectionName;
		this.previewUrl = previewUrl;
	}

	public String getCcollectionName() {
		return collectionName;
	}

	public String getPreviewUrl() {
		return previewUrl;
	}

}
