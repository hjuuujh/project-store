package com.zerobase.storereservation.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class KaKakoApi {
    // 카카오 api 이용해 도로명 주소로 요청하면 위도, 경도 반환 받음
    @Value("${kakao.api.key}")
    private String apiKey; // static 변수면 @Value로 못가져옴

    public List<Float> getCoordinateFromApi(String address) throws IOException, ParseException {
        address = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String apiUrl = "https://dapi.kakao.com/v2/local/search/address.json?query=" + address;

        HttpURLConnection conn = null;
        StringBuilder response = new StringBuilder();

        String auth = "KakaoAK " + apiKey;

        URL url = new URL(apiUrl);

        conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Requested-With", "curl");
        conn.setRequestProperty("Authorization", auth);

        conn.setDoOutput(true);

        int responseCode = conn.getResponseCode();
        if (responseCode == 400) {
            System.out.println("400:: 해당 명령을 실행할 수 없음");
        } else if (responseCode == 401) {
            System.out.println("401:: Authorization가 잘못됨");
        } else if (responseCode == 500) {
            System.out.println("500:: 서버 에러, 문의 필요");
        } else {

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
        }

        return parseCoordinate(response.toString());
    }

    // json 데이터를 파싱해 위도, 경도 정보만 리스트에 저장
    private List<Float> parseCoordinate(String json) throws ParseException {
        List<Float> coordinate = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject document = (JSONObject) parser.parse(String.valueOf(json));
        JSONArray jsonArray = (JSONArray) document.get("documents");
        JSONObject position = (JSONObject) jsonArray.get(0);
        float lon = Float.parseFloat((String) position.get("x"));
        float lat = Float.parseFloat((String) position.get("y"));
        coordinate.add(lon);
        coordinate.add(lat);

        return coordinate;
    }

}
