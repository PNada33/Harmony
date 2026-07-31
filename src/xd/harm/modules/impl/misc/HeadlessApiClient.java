package xd.harm.modules.impl.misc;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HeadlessApiClient {
    public static class Bot {
        public String name="Unknown", state="offline", roles="—", position="—", task="—", error="";
        public float health=-1, food=-1; public int ping=-1;
        public boolean online(){String s=state.toLowerCase(Locale.ROOT);return s.contains("online")||s.contains("spawn")||s.contains("world")||s.equals("ready")||s.equals("verified");}
        public boolean checking(){String s=state.toLowerCase(Locale.ROOT);return s.contains("connect")||s.contains("verif")||s.contains("check");}
    }
    private volatile String base="http://127.0.0.1:3001", message="Ожидание подключения", server="offline", llm="unknown";
    private volatile boolean connected, loading;
    private volatile List<Bot> bots=Collections.emptyList();
    private volatile List<String> logs=Collections.emptyList();
    public String base(){return base;} public String message(){return message;} public String server(){return server;} public String llm(){return llm;}
    public boolean connected(){return connected;} public boolean loading(){return loading;} public List<Bot> bots(){return bots;} public List<String> logs(){return logs;}
    public void setBase(String value){if(value==null||value.trim().isEmpty())return; value=value.trim(); if(!value.startsWith("http"))value="http://"+value; while(value.endsWith("/"))value=value.substring(0,value.length()-1); base=value;}
    public void refresh(){
        if(loading)return; loading=true;
        CompletableFuture.runAsync(()->{try{
            JsonElement root=getAny("/api/status","/api/overview","/api/snapshot","/status");
            JsonObject o=root.isJsonObject()?root.getAsJsonObject():new JsonObject();
            server=str(o,"serverState",str(o,"state","online")); llm=str(o,"llmState",str(o,"llm","unknown"));
            JsonElement be=find(o,"bots"); if(be==null)try{be=getAny("/api/bots","/bots");}catch(Exception ignored){}
            bots=parseBots(be);
            JsonElement le=find(o,"logs"); if(le==null)try{le=getAny("/api/logs?limit=100","/api/events?limit=100");}catch(Exception ignored){}
            logs=parseLogs(le); connected=true; message="Подключено";
        }catch(Exception e){connected=false;message=cut(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(),80);}finally{loading=false;}});
    }
    public void command(String action,String target,String value){
        CompletableFuture.runAsync(()->{try{
            JsonObject b=new JsonObject(); b.addProperty("action",action);b.addProperty("target",target==null?"all":target);b.addProperty("value",value==null?"":value);
            String cmd=action+(target==null||target.isEmpty()?"":" "+target)+(value==null||value.isEmpty()?"":" "+value);b.addProperty("command",cmd.trim());
            request("POST","/api/command",b.toString());message="Команда отправлена";refresh();
        }catch(Exception e){message=cut(e.getMessage()==null?"Ошибка API":e.getMessage(),80);}});
    }
    private JsonElement getAny(String... paths)throws Exception{Exception last=null;for(String p:paths)try{return request("GET",p,null);}catch(Exception e){last=e;}throw last;}
    private JsonElement request(String method,String path,String body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(base+path).openConnection();c.setRequestMethod(method);c.setConnectTimeout(1700);c.setReadTimeout(2300);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Content-Type","application/json; charset=utf-8");
        if(body!=null){c.setDoOutput(true);byte[] d=body.getBytes(StandardCharsets.UTF_8);try(OutputStream os=c.getOutputStream()){os.write(d);}}
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();StringBuilder s=new StringBuilder();if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line);}
        c.disconnect();if(code<200||code>=300)throw new IOException("HTTP "+code+" "+path);return s.length()==0?new JsonObject():new JsonParser().parse(s.toString());
    }
    private List<Bot> parseBots(JsonElement e){if(e==null)return Collections.emptyList();if(e.isJsonObject()){JsonElement n=find(e.getAsJsonObject(),"bots");if(n!=null)e=n;}if(!e.isJsonArray())return Collections.emptyList();List<Bot> out=new ArrayList<>();for(JsonElement v:e.getAsJsonArray()){if(!v.isJsonObject())continue;JsonObject o=v.getAsJsonObject();Bot b=new Bot();b.name=str(o,"username",str(o,"name","Unknown"));b.state=str(o,"state",str(o,"status","offline"));b.roles=roles(o);b.position=str(o,"position","—");b.task=str(o,"task",str(o,"currentTask","—"));b.error=str(o,"reason",str(o,"lastError",""));b.health=num(o,"health",-1);b.food=num(o,"food",-1);b.ping=(int)num(o,"ping",-1);out.add(b);}return Collections.unmodifiableList(out);}
    private List<String> parseLogs(JsonElement e){if(e==null)return Collections.emptyList();if(e.isJsonObject()){JsonElement n=find(e.getAsJsonObject(),"logs");if(n==null)n=find(e.getAsJsonObject(),"events");if(n!=null)e=n;}if(!e.isJsonArray())return Collections.emptyList();List<String> out=new ArrayList<>();for(JsonElement v:e.getAsJsonArray()){if(v.isJsonPrimitive())out.add(v.getAsString());else if(v.isJsonObject()){JsonObject o=v.getAsJsonObject();out.add((str(o,"bot","").isEmpty()?"":"["+str(o,"bot","")+"] ")+str(o,"message",str(o,"text",v.toString())));}}return Collections.unmodifiableList(out);}
    private JsonElement find(JsonObject o,String k){if(o==null)return null;if(o.has(k))return o.get(k);if(o.has("data")&&o.get("data").isJsonObject()&&o.getAsJsonObject("data").has(k))return o.getAsJsonObject("data").get(k);return null;}
    private String str(JsonObject o,String k,String d){try{JsonElement e=find(o,k);return e==null||e.isJsonNull()?d:e.isJsonPrimitive()?e.getAsString():e.toString();}catch(Exception x){return d;}}
    private float num(JsonObject o,String k,float d){try{JsonElement e=find(o,k);return e==null?d:e.getAsFloat();}catch(Exception x){return d;}}
    private String roles(JsonObject o){JsonElement e=find(o,"roles");if(e==null)return"—";if(!e.isJsonArray())return e.getAsString();StringBuilder s=new StringBuilder();for(JsonElement v:e.getAsJsonArray()){if(s.length()>0)s.append(" + ");s.append(v.getAsString());}return s.length()==0?"—":s.toString();}
    private String cut(String s,int n){return s==null?"":s.length()<=n?s:s.substring(0,n-1)+"…";}
}
