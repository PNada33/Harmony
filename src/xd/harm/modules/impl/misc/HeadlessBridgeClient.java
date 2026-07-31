package xd.harm.modules.impl.misc;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HeadlessBridgeClient {
    private static final String BRIDGE = "http://127.0.0.1:32145";
    public static class Bot {
        public String name="Unknown", state="offline", roles="—", position="—", task="—", error="", protocol="—", window="нет", server="—";
        public float health=-1, food=-1; public int ping=-1, inventoryCount=0; public long connectDelayMs=0,followRefreshMs=0;
        public boolean online(){String s=state.toLowerCase(Locale.ROOT);return s.contains("online")||s.contains("moving")||s.contains("follow")||s.equals("ready")||s.equals("verified");}
        public boolean checking(){String s=state.toLowerCase(Locale.ROOT);return s.contains("connect")||s.contains("verif")||s.contains("check");}
    }
    private volatile String message="HarmonyHeadless не запущен", actionMessage="", service="HarmonyHeadless", server="—", team="—", llm="выключен";
    private volatile boolean connected, loading;
    private volatile long uptimeMs;
    private volatile List<Bot> bots=Collections.emptyList();
    private volatile List<String> logs=Collections.emptyList();
    public String message(){return message;} public String actionMessage(){return actionMessage;} public void setActionMessage(String text){actionMessage=text==null?"":text;} public String service(){return service;} public String server(){return server;} public String team(){return team;} public String llm(){return llm;}
    public long uptimeMs(){return uptimeMs;} public boolean connected(){return connected;} public boolean loading(){return loading;} public List<Bot> bots(){return bots;} public List<String> logs(){return logs;}
    public synchronized void addLocalBot(String name,String address){if(name==null||name.trim().isEmpty())return;Bot b=new Bot();b.name=name.trim();b.state="starting";b.roles="Ожидание Headless";b.position="—";b.server=address==null?"—":address.trim();b.task="Запуск";List<Bot> next=new ArrayList<>();for(Bot old:bots)if(!old.name.equalsIgnoreCase(b.name))next.add(old);next.add(b);bots=Collections.unmodifiableList(next);message="Бот добавлен: "+b.name;}
    public synchronized void removeLocalBot(String name){List<Bot> next=new ArrayList<>();for(Bot bot:bots)if(!bot.name.equalsIgnoreCase(name))next.add(bot);bots=Collections.unmodifiableList(next);message="Бот удалён: "+name;}
    public synchronized void replaceLocalBot(String oldName,String newName,String address){List<Bot> next=new ArrayList<>();boolean replaced=false;for(Bot bot:bots){if(bot.name.equalsIgnoreCase(oldName)){Bot b=new Bot();b.name=newName;b.state="starting";b.roles=bot.roles;b.position="—";b.task="Переподключение";b.server=address;next.add(b);replaced=true;}else next.add(bot);}if(!replaced){Bot b=new Bot();b.name=newName;b.state="starting";b.server=address;next.add(b);}bots=Collections.unmodifiableList(next);message="Бот изменён: "+newName;}
    public void refresh(){if(loading)return;loading=true;CompletableFuture.runAsync(()->{try{JsonObject root=get("/snapshot").getAsJsonObject();JsonObject svc=obj(root,"service");service=str(svc,"name","HarmonyHeadless");uptimeMs=longNum(svc,"uptimeMs",0);JsonObject srv=obj(root,"server");server=str(srv,"host","—")+":"+str(srv,"port","25565");JsonObject tm=obj(root,"team");team=str(tm,"id","—");llm=bool(tm,"llmEnabled",false)?"включен":"выключен";bots=parseBots(root.get("bots"));logs=parseLogs(root.get("logs"));connected=true;message="Встроенное управление активно";}catch(Exception e){connected=false;message=cut(e.getMessage()==null?"HarmonyHeadless не запущен":e.getMessage(),76);}finally{loading=false;}});}
    public void command(String action,String target,String value){actionMessage="Выполняется: "+action+" -> "+(target==null?"all":target);CompletableFuture.runAsync(()->{try{JsonObject b=new JsonObject();b.addProperty("action",action);b.addProperty("target",target==null?"all":target);b.addProperty("value",value==null?"":value);request("POST","/command",b.toString());actionMessage="Готово: "+action+" -> "+(target==null?"all":target);refresh();}catch(Exception e){actionMessage="Ошибка: "+cut(e.getMessage()==null?"Ошибка управления":e.getMessage(),96);}});}
    private JsonElement get(String path)throws Exception{return request("GET",path,null);}
    private JsonElement request(String method,String path,String body)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(BRIDGE+path).openConnection();c.setRequestMethod(method);c.setConnectTimeout(850);c.setReadTimeout(1800);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Content-Type","application/json; charset=utf-8");if(body!=null){c.setDoOutput(true);try(OutputStream os=c.getOutputStream()){os.write(body.getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();StringBuilder out=new StringBuilder();if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)out.append(line);}String response=out.toString();c.disconnect();if(code<200||code>=300){String detail=response;try{JsonObject errorJson=new JsonParser().parse(response).getAsJsonObject();detail=str(errorJson,"error",response);}catch(Exception ignored){}throw new IOException("HTTP "+code+(detail.isEmpty()?"":": "+detail));}return response.isEmpty()?new JsonObject():new JsonParser().parse(response);}
    private List<Bot> parseBots(JsonElement e){if(e==null||!e.isJsonArray())return Collections.emptyList();List<Bot> out=new ArrayList<>();for(JsonElement v:e.getAsJsonArray()){if(!v.isJsonObject())continue;JsonObject o=v.getAsJsonObject();Bot b=new Bot();b.name=str(o,"username","Unknown");b.state=str(o,"state","offline");b.roles=roles(o.get("roles"));b.position=str(o,"position","—");b.task=str(o,"task","—");b.error=str(o,"lastError","");b.protocol=str(o,"protocol","—");b.window=str(o,"windowTitle","нет");b.server=str(o,"server",server);b.health=num(o,"health",-1);b.food=num(o,"food",-1);b.ping=(int)num(o,"ping",-1);b.inventoryCount=(int)num(o,"inventoryCount",0);b.connectDelayMs=longNum(o,"connectDelayMs",0);b.followRefreshMs=longNum(o,"followRefreshMs",0);out.add(b);}return Collections.unmodifiableList(out);}
    private List<String> parseLogs(JsonElement e){if(e==null||!e.isJsonArray())return Collections.emptyList();List<String> out=new ArrayList<>();for(JsonElement v:e.getAsJsonArray()){if(v.isJsonPrimitive())out.add(v.getAsString());else if(v.isJsonObject()){JsonObject o=v.getAsJsonObject();out.add("["+str(o,"time","")+"] ["+str(o,"level","info")+"] "+str(o,"message",""));}}return Collections.unmodifiableList(out);}
    private String roles(JsonElement e){if(e==null||e.isJsonNull())return"—";if(!e.isJsonArray())return e.getAsString();StringBuilder s=new StringBuilder();for(JsonElement v:e.getAsJsonArray()){if(s.length()>0)s.append(" + ");s.append(v.getAsString());}return s.length()==0?"—":s.toString();}
    private JsonObject obj(JsonObject o,String k){return o.has(k)&&o.get(k).isJsonObject()?o.getAsJsonObject(k):new JsonObject();}
    private String str(JsonObject o,String k,String d){try{JsonElement e=o.get(k);return e==null||e.isJsonNull()?d:e.getAsString();}catch(Exception x){return d;}}
    private float num(JsonObject o,String k,float d){try{JsonElement e=o.get(k);return e==null||e.isJsonNull()?d:e.getAsFloat();}catch(Exception x){return d;}}
    private long longNum(JsonObject o,String k,long d){try{JsonElement e=o.get(k);return e==null||e.isJsonNull()?d:e.getAsLong();}catch(Exception x){return d;}}
    private boolean bool(JsonObject o,String k,boolean d){try{JsonElement e=o.get(k);return e==null||e.isJsonNull()?d:e.getAsBoolean();}catch(Exception x){return d;}}
    private String cut(String s,int n){return s==null?"":s.length()<=n?s:s.substring(0,n-1)+"…";}
}
