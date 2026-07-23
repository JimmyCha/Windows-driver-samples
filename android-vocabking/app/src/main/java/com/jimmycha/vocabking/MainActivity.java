package com.jimmycha.vocabking;

import android.app.*;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.content.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    static class Word { int id; String word, pos, meaning; }
    private final ArrayList<Word> all = new ArrayList<>(), lesson = new ArrayList<>(), quiz = new ArrayList<>();
    private final ArrayList<String> answers = new ArrayList<>();
    private TextToSpeech tts; private boolean ttsReady=false, quizActive=false;
    private LinearLayout root, content; private Button lessonBtn, quizBtn, historyBtn;
    private int qIndex=0; private EditText answerBox;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("scores",MODE_PRIVATE); loadWords(); tts=new TextToSpeech(this,this); buildShell(); showLesson(); }
    private TextView tv(String s,int sp){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setPadding(12,10,12,10); return v; }
    private Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private void buildShell(){
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,18,18,18); scroll.addView(root);
        TextView title=tv("國中單字王朗讀測驗 v1.2",24); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        lessonBtn=btn("單字教學"); quizBtn=btn("開始5題測驗"); historyBtn=btn("成績紀錄");
        nav.addView(lessonBtn,new LinearLayout.LayoutParams(0,-2,1)); nav.addView(quizBtn,new LinearLayout.LayoutParams(0,-2,1)); nav.addView(historyBtn,new LinearLayout.LayoutParams(0,-2,1)); root.addView(nav);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,16,0,30); root.addView(content);
        lessonBtn.setOnClickListener(v->{ if(!quizActive) showLesson(); }); quizBtn.setOnClickListener(v->startQuiz()); historyBtn.setOnClickListener(v->{ if(!quizActive) showHistory(); }); setContentView(scroll);
    }
    private void clear(){ content.removeAllViews(); }
    private void showLesson(){ quizActive=false; setNavEnabled(true); clear(); lesson.clear(); Collections.shuffle(all); lesson.addAll(all.subList(0,Math.min(10,all.size()))); TextView h=tv("本次隨機 10 個單字",20); h.setTypeface(null,Typeface.BOLD); content.addView(h); Button allSpeak=btn("依序朗讀10個單字"); content.addView(allSpeak); allSpeak.setOnClickListener(v->speakLesson()); for(Word w:lesson){ LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(8,8,8,8); TextView a=tv(w.word+"　"+w.pos,19); a.setTypeface(null,Typeface.BOLD); row.addView(a); row.addView(tv(w.meaning,17)); LinearLayout actions=new LinearLayout(this); Button speak=btn("朗讀"); Button slow=btn("慢速"); Button spell=btn("拼字"); actions.addView(speak);actions.addView(slow);actions.addView(spell); row.addView(actions); content.addView(row); speak.setOnClickListener(v->speak(w.word,0.85f)); slow.setOnClickListener(v->speak(w.word,0.55f)); spell.setOnClickListener(v->speak(w.word.replace(""," "),0.65f)); } }
    private void speakLesson(){ if(!ttsReady){ toast("Android 語音引擎尚未就緒"); return;} tts.stop(); for(int i=0;i<lesson.size();i++) tts.speak(lesson.get(i).word,TextToSpeech.QUEUE_ADD,null,"lesson"+i); }
    private void speak(String s,float rate){ if(!ttsReady){ toast("請先安裝或啟用 Android 英文文字轉語音服務"); return;} tts.setSpeechRate(rate); tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"speak"); }
    private void startQuiz(){ if(lesson.size()<5) showLesson(); quizActive=true; setNavEnabled(false); clear(); quiz.clear(); answers.clear(); qIndex=0; ArrayList<Word> tmp=new ArrayList<>(lesson); Collections.shuffle(tmp); quiz.addAll(tmp.subList(0,5)); showQuestion(); }
    private void showQuestion(){ clear(); Word w=quiz.get(qIndex); content.addView(tv("第 "+(qIndex+1)+" / 5 題",18)); TextView q=tv("請聽發音，輸入英文單字或片語",21); q.setTypeface(null,Typeface.BOLD); content.addView(q); Button replay=btn("重新朗讀"); content.addView(replay); replay.setOnClickListener(v->speak(w.word,0.75f)); answerBox=new EditText(this); answerBox.setHint("輸入答案"); answerBox.setTextSize(20); content.addView(answerBox); Button next=btn(qIndex==4?"完成並查看分數":"送出並到下一題"); content.addView(next); next.setOnClickListener(v->{ String ans=answerBox.getText().toString().trim(); if(ans.isEmpty()){toast("請先輸入答案");return;} answers.add(ans); if(qIndex<4){qIndex++;showQuestion(); new Handler(Looper.getMainLooper()).postDelayed(()->speak(quiz.get(qIndex).word,0.75f),250);} else finishQuiz(); }); new Handler(Looper.getMainLooper()).postDelayed(()->speak(w.word,0.75f),300); }
    private void finishQuiz(){ int correct=0; StringBuilder detail=new StringBuilder(); for(int i=0;i<5;i++){ Word w=quiz.get(i); boolean ok=norm(answers.get(i)).equals(norm(w.word)); if(ok)correct++; detail.append(i+1).append(". ").append(w.word).append(" = ").append(w.meaning).append("\n你的答案：").append(answers.get(i)).append(ok?"　✓":"　✗").append("\n\n"); } int score=correct*20; String comment=score==100?"非常優秀，5題全部答對！":score>=80?"表現很好，再複習錯題就更穩定。":score>=60?"已有基本掌握，建議重新朗讀並加強錯題。":"需要多練習，建議先回到單字教學反覆聽讀。"; saveScore(score); quizActive=false; setNavEnabled(true); clear(); TextView h=tv("測驗完成："+score+" 分",25); h.setTypeface(null,Typeface.BOLD); content.addView(h); content.addView(tv(comment,18)); content.addView(tv(detail.toString(),17)); Button back=btn("回到單字教學"); content.addView(back); back.setOnClickListener(v->showLesson()); }
    private void showHistory(){ clear(); content.addView(tv("成績紀錄",23)); String raw=prefs.getString("history",""); content.addView(tv(raw.isEmpty()?"尚無測驗紀錄":raw,17)); }
    private void saveScore(int score){ String old=prefs.getString("history",""); String line=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.TAIWAN).format(new Date())+"　"+score+" 分\n"; prefs.edit().putString("history",line+old).apply(); }
    private void setNavEnabled(boolean enabled){ lessonBtn.setEnabled(enabled); historyBtn.setEnabled(enabled); quizBtn.setEnabled(enabled); }
    private String norm(String s){ return s.toLowerCase(Locale.US).trim().replaceAll("\\s+"," "); }
    private void loadWords(){ try(InputStream in=getAssets().open("vocabulary.json")){ ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0) out.write(buf,0,n); JSONArray a=new JSONArray(out.toString(StandardCharsets.UTF_8.name())); for(int i=0;i<a.length();i++){ JSONObject o=a.getJSONObject(i); Word w=new Word(); w.id=o.getInt("id");w.word=o.getString("word");w.pos=o.getString("pos");w.meaning=o.getString("meaning");all.add(w);} }catch(Exception e){ new AlertDialog.Builder(this).setTitle("題庫載入失敗").setMessage(e.toString()).setPositiveButton("確定",null).show(); } }
    private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    @Override public void onInit(int status){ if(status==TextToSpeech.SUCCESS){ int r=tts.setLanguage(Locale.US); ttsReady=r!=TextToSpeech.LANG_MISSING_DATA&&r!=TextToSpeech.LANG_NOT_SUPPORTED; tts.setSpeechRate(0.8f); if(!ttsReady) toast("手機缺少英文語音資料，請到文字轉語音設定下載英文語音"); } else toast("Android 文字轉語音初始化失敗"); }
    @Override protected void onDestroy(){ if(tts!=null){tts.stop();tts.shutdown();} super.onDestroy(); }
    @Override public void onBackPressed(){ if(quizActive){ new AlertDialog.Builder(this).setTitle("測驗進行中").setMessage("離開會放棄本次測驗，確定嗎？").setNegativeButton("取消",null).setPositiveButton("離開",(d,w)->{quizActive=false;setNavEnabled(true);showLesson();}).show(); } else super.onBackPressed(); }
}
