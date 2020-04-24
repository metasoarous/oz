goog.provide('shadow.cljs.devtools.client.node');
goog.require('cljs.core');
goog.require('shadow.cljs.devtools.client.env');
goog.require('shadow.js.shim.module$ws');
goog.require('cljs.reader');
goog.require('goog.object');
if((typeof shadow !== 'undefined') && (typeof shadow.cljs !== 'undefined') && (typeof shadow.cljs.devtools !== 'undefined') && (typeof shadow.cljs.devtools.client !== 'undefined') && (typeof shadow.cljs.devtools.client.node !== 'undefined') && (typeof shadow.cljs.devtools.client.node.client_id !== 'undefined')){
} else {
shadow.cljs.devtools.client.node.client_id = cljs.core.random_uuid();
}
if((typeof shadow !== 'undefined') && (typeof shadow.cljs !== 'undefined') && (typeof shadow.cljs.devtools !== 'undefined') && (typeof shadow.cljs.devtools.client !== 'undefined') && (typeof shadow.cljs.devtools.client.node !== 'undefined') && (typeof shadow.cljs.devtools.client.node.ws_ref !== 'undefined')){
} else {
shadow.cljs.devtools.client.node.ws_ref = cljs.core.volatile_BANG_(null);
}
shadow.cljs.devtools.client.node.ws_close = (function shadow$cljs$devtools$client$node$ws_close(){
var temp__5739__auto__ = cljs.core.deref(shadow.cljs.devtools.client.node.ws_ref);
if((temp__5739__auto__ == null)){
return null;
} else {
var tcp = temp__5739__auto__;
tcp.close();

return cljs.core.vreset_BANG_(shadow.cljs.devtools.client.node.ws_ref,null);
}
});
shadow.cljs.devtools.client.node.ws_msg = (function shadow$cljs$devtools$client$node$ws_msg(msg){
var temp__5739__auto__ = cljs.core.deref(shadow.cljs.devtools.client.node.ws_ref);
if((temp__5739__auto__ == null)){
return null;
} else {
var ws = temp__5739__auto__;
return ws.send(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([msg], 0)),(function (err){
if(cljs.core.truth_(err)){
return console.error("REPL msg send failed",err);
} else {
return null;
}
}));
}
});
shadow.cljs.devtools.client.node.node_eval = (function shadow$cljs$devtools$client$node$node_eval(p__29739){
var map__29740 = p__29739;
var map__29740__$1 = (((((!((map__29740 == null))))?(((((map__29740.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29740.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29740):map__29740);
var msg = map__29740__$1;
var js = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29740__$1,new cljs.core.Keyword(null,"js","js",1768080579));
var source_map_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29740__$1,new cljs.core.Keyword(null,"source-map-json","source-map-json",-299460036));
var result = SHADOW_NODE_EVAL(js,source_map_json);
return result;
});
shadow.cljs.devtools.client.node.is_loaded_QMARK_ = (function shadow$cljs$devtools$client$node$is_loaded_QMARK_(src){
return goog.object.get(SHADOW_IMPORTED,src) === true;
});
shadow.cljs.devtools.client.node.closure_import = (function shadow$cljs$devtools$client$node$closure_import(src){
if(typeof src === 'string'){
} else {
throw (new Error("Assert failed: (string? src)"));
}

return SHADOW_IMPORT(src);
});
shadow.cljs.devtools.client.node.repl_init = (function shadow$cljs$devtools$client$node$repl_init(p__29749,done){
var map__29750 = p__29749;
var map__29750__$1 = (((((!((map__29750 == null))))?(((((map__29750.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29750.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29750):map__29750);
var msg = map__29750__$1;
var id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29750__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var repl_state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29750__$1,new cljs.core.Keyword(null,"repl-state","repl-state",-1733780387));
var map__29752 = repl_state;
var map__29752__$1 = (((((!((map__29752 == null))))?(((((map__29752.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29752.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29752):map__29752);
var repl_sources = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29752__$1,new cljs.core.Keyword(null,"repl-sources","repl-sources",723867535));
var seq__29755_29851 = cljs.core.seq(repl_sources);
var chunk__29757_29852 = null;
var count__29758_29853 = (0);
var i__29759_29854 = (0);
while(true){
if((i__29759_29854 < count__29758_29853)){
var map__29793_29855 = chunk__29757_29852.cljs$core$IIndexed$_nth$arity$2(null,i__29759_29854);
var map__29793_29856__$1 = (((((!((map__29793_29855 == null))))?(((((map__29793_29855.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29793_29855.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29793_29855):map__29793_29855);
var src_29857 = map__29793_29856__$1;
var output_name_29858 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29793_29856__$1,new cljs.core.Keyword(null,"output-name","output-name",-1769107767));
if((!(shadow.cljs.devtools.client.node.is_loaded_QMARK_(output_name_29858)))){
shadow.cljs.devtools.client.node.closure_import(output_name_29858);


var G__29859 = seq__29755_29851;
var G__29860 = chunk__29757_29852;
var G__29861 = count__29758_29853;
var G__29862 = (i__29759_29854 + (1));
seq__29755_29851 = G__29859;
chunk__29757_29852 = G__29860;
count__29758_29853 = G__29861;
i__29759_29854 = G__29862;
continue;
} else {
var G__29863 = seq__29755_29851;
var G__29864 = chunk__29757_29852;
var G__29865 = count__29758_29853;
var G__29866 = (i__29759_29854 + (1));
seq__29755_29851 = G__29863;
chunk__29757_29852 = G__29864;
count__29758_29853 = G__29865;
i__29759_29854 = G__29866;
continue;
}
} else {
var temp__5735__auto___29867 = cljs.core.seq(seq__29755_29851);
if(temp__5735__auto___29867){
var seq__29755_29868__$1 = temp__5735__auto___29867;
if(cljs.core.chunked_seq_QMARK_(seq__29755_29868__$1)){
var c__4609__auto___29869 = cljs.core.chunk_first(seq__29755_29868__$1);
var G__29870 = cljs.core.chunk_rest(seq__29755_29868__$1);
var G__29871 = c__4609__auto___29869;
var G__29872 = cljs.core.count(c__4609__auto___29869);
var G__29873 = (0);
seq__29755_29851 = G__29870;
chunk__29757_29852 = G__29871;
count__29758_29853 = G__29872;
i__29759_29854 = G__29873;
continue;
} else {
var map__29795_29874 = cljs.core.first(seq__29755_29868__$1);
var map__29795_29875__$1 = (((((!((map__29795_29874 == null))))?(((((map__29795_29874.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29795_29874.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29795_29874):map__29795_29874);
var src_29876 = map__29795_29875__$1;
var output_name_29877 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29795_29875__$1,new cljs.core.Keyword(null,"output-name","output-name",-1769107767));
if((!(shadow.cljs.devtools.client.node.is_loaded_QMARK_(output_name_29877)))){
shadow.cljs.devtools.client.node.closure_import(output_name_29877);


var G__29879 = cljs.core.next(seq__29755_29868__$1);
var G__29880 = null;
var G__29881 = (0);
var G__29882 = (0);
seq__29755_29851 = G__29879;
chunk__29757_29852 = G__29880;
count__29758_29853 = G__29881;
i__29759_29854 = G__29882;
continue;
} else {
var G__29883 = cljs.core.next(seq__29755_29868__$1);
var G__29884 = null;
var G__29885 = (0);
var G__29886 = (0);
seq__29755_29851 = G__29883;
chunk__29757_29852 = G__29884;
count__29758_29853 = G__29885;
i__29759_29854 = G__29886;
continue;
}
}
} else {
}
}
break;
}

shadow.cljs.devtools.client.node.ws_msg(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("repl","init-complete","repl/init-complete",-162252879),new cljs.core.Keyword(null,"id","id",-1388402092),id], null));

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
});
shadow.cljs.devtools.client.node.repl_invoke = (function shadow$cljs$devtools$client$node$repl_invoke(p__29797){
var map__29798 = p__29797;
var map__29798__$1 = (((((!((map__29798 == null))))?(((((map__29798.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29798.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29798):map__29798);
var msg = map__29798__$1;
var id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29798__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var result = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(shadow.cljs.devtools.client.env.repl_call((function (){
return shadow.cljs.devtools.client.node.node_eval(msg);
}),shadow.cljs.devtools.client.env.repl_error),new cljs.core.Keyword(null,"id","id",-1388402092),id);
return shadow.cljs.devtools.client.node.ws_msg(result);
});
shadow.cljs.devtools.client.node.repl_set_ns = (function shadow$cljs$devtools$client$node$repl_set_ns(p__29800){
var map__29801 = p__29800;
var map__29801__$1 = (((((!((map__29801 == null))))?(((((map__29801.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29801.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29801):map__29801);
var msg = map__29801__$1;
var id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29801__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
return shadow.cljs.devtools.client.node.ws_msg(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("repl","set-ns-complete","repl/set-ns-complete",680944662),new cljs.core.Keyword(null,"id","id",-1388402092),id], null));
});
shadow.cljs.devtools.client.node.repl_require = (function shadow$cljs$devtools$client$node$repl_require(p__29803,done){
var map__29804 = p__29803;
var map__29804__$1 = (((((!((map__29804 == null))))?(((((map__29804.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29804.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29804):map__29804);
var msg = map__29804__$1;
var id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29804__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var sources = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29804__$1,new cljs.core.Keyword(null,"sources","sources",-321166424));
var reload_namespaces = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29804__$1,new cljs.core.Keyword(null,"reload-namespaces","reload-namespaces",250210134));
try{var seq__29807_29887 = cljs.core.seq(sources);
var chunk__29808_29888 = null;
var count__29809_29889 = (0);
var i__29810_29890 = (0);
while(true){
if((i__29810_29890 < count__29809_29889)){
var map__29815_29891 = chunk__29808_29888.cljs$core$IIndexed$_nth$arity$2(null,i__29810_29890);
var map__29815_29892__$1 = (((((!((map__29815_29891 == null))))?(((((map__29815_29891.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29815_29891.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29815_29891):map__29815_29891);
var src_29893 = map__29815_29892__$1;
var provides_29894 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29815_29892__$1,new cljs.core.Keyword(null,"provides","provides",-1634397992));
var output_name_29895 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29815_29892__$1,new cljs.core.Keyword(null,"output-name","output-name",-1769107767));
if(cljs.core.truth_((function (){var or__4185__auto__ = (!(shadow.cljs.devtools.client.node.is_loaded_QMARK_(output_name_29895)));
if(or__4185__auto__){
return or__4185__auto__;
} else {
return cljs.core.some(reload_namespaces,provides_29894);
}
})())){
shadow.cljs.devtools.client.node.closure_import(output_name_29895);
} else {
}


var G__29896 = seq__29807_29887;
var G__29897 = chunk__29808_29888;
var G__29898 = count__29809_29889;
var G__29899 = (i__29810_29890 + (1));
seq__29807_29887 = G__29896;
chunk__29808_29888 = G__29897;
count__29809_29889 = G__29898;
i__29810_29890 = G__29899;
continue;
} else {
var temp__5735__auto___29900 = cljs.core.seq(seq__29807_29887);
if(temp__5735__auto___29900){
var seq__29807_29901__$1 = temp__5735__auto___29900;
if(cljs.core.chunked_seq_QMARK_(seq__29807_29901__$1)){
var c__4609__auto___29902 = cljs.core.chunk_first(seq__29807_29901__$1);
var G__29903 = cljs.core.chunk_rest(seq__29807_29901__$1);
var G__29904 = c__4609__auto___29902;
var G__29905 = cljs.core.count(c__4609__auto___29902);
var G__29906 = (0);
seq__29807_29887 = G__29903;
chunk__29808_29888 = G__29904;
count__29809_29889 = G__29905;
i__29810_29890 = G__29906;
continue;
} else {
var map__29817_29907 = cljs.core.first(seq__29807_29901__$1);
var map__29817_29908__$1 = (((((!((map__29817_29907 == null))))?(((((map__29817_29907.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29817_29907.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29817_29907):map__29817_29907);
var src_29909 = map__29817_29908__$1;
var provides_29910 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29817_29908__$1,new cljs.core.Keyword(null,"provides","provides",-1634397992));
var output_name_29911 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29817_29908__$1,new cljs.core.Keyword(null,"output-name","output-name",-1769107767));
if(cljs.core.truth_((function (){var or__4185__auto__ = (!(shadow.cljs.devtools.client.node.is_loaded_QMARK_(output_name_29911)));
if(or__4185__auto__){
return or__4185__auto__;
} else {
return cljs.core.some(reload_namespaces,provides_29910);
}
})())){
shadow.cljs.devtools.client.node.closure_import(output_name_29911);
} else {
}


var G__29912 = cljs.core.next(seq__29807_29901__$1);
var G__29913 = null;
var G__29914 = (0);
var G__29915 = (0);
seq__29807_29887 = G__29912;
chunk__29808_29888 = G__29913;
count__29809_29889 = G__29914;
i__29810_29890 = G__29915;
continue;
}
} else {
}
}
break;
}

shadow.cljs.devtools.client.node.ws_msg(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("repl","require-complete","repl/require-complete",-2140254719),new cljs.core.Keyword(null,"id","id",-1388402092),id], null));
}catch (e29806){var e_29916 = e29806;
console.error("repl/require failed",e_29916);

shadow.cljs.devtools.client.node.ws_msg(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("repl","require-error","repl/require-error",1689310021),new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"error","error",-978969032),e_29916.message], null));
}
return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
});
shadow.cljs.devtools.client.node.build_complete = (function shadow$cljs$devtools$client$node$build_complete(p__29819){
var map__29820 = p__29819;
var map__29820__$1 = (((((!((map__29820 == null))))?(((((map__29820.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29820.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29820):map__29820);
var msg = map__29820__$1;
var info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29820__$1,new cljs.core.Keyword(null,"info","info",-317069002));
var reload_info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29820__$1,new cljs.core.Keyword(null,"reload-info","reload-info",1648088086));
var map__29823 = info;
var map__29823__$1 = (((((!((map__29823 == null))))?(((((map__29823.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29823.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29823):map__29823);
var sources = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29823__$1,new cljs.core.Keyword(null,"sources","sources",-321166424));
var compiled = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29823__$1,new cljs.core.Keyword(null,"compiled","compiled",850043082));
var warnings = cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.distinct.cljs$core$IFn$_invoke$arity$1((function (){var iter__4582__auto__ = (function shadow$cljs$devtools$client$node$build_complete_$_iter__29825(s__29826){
return (new cljs.core.LazySeq(null,(function (){
var s__29826__$1 = s__29826;
while(true){
var temp__5735__auto__ = cljs.core.seq(s__29826__$1);
if(temp__5735__auto__){
var xs__6292__auto__ = temp__5735__auto__;
var map__29831 = cljs.core.first(xs__6292__auto__);
var map__29831__$1 = (((((!((map__29831 == null))))?(((((map__29831.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29831.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29831):map__29831);
var src = map__29831__$1;
var resource_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29831__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
var warnings = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29831__$1,new cljs.core.Keyword(null,"warnings","warnings",-735437651));
if(cljs.core.not(new cljs.core.Keyword(null,"from-jar","from-jar",1050932827).cljs$core$IFn$_invoke$arity$1(src))){
var iterys__4578__auto__ = ((function (s__29826__$1,map__29831,map__29831__$1,src,resource_name,warnings,xs__6292__auto__,temp__5735__auto__,map__29823,map__29823__$1,sources,compiled,map__29820,map__29820__$1,msg,info,reload_info){
return (function shadow$cljs$devtools$client$node$build_complete_$_iter__29825_$_iter__29827(s__29828){
return (new cljs.core.LazySeq(null,((function (s__29826__$1,map__29831,map__29831__$1,src,resource_name,warnings,xs__6292__auto__,temp__5735__auto__,map__29823,map__29823__$1,sources,compiled,map__29820,map__29820__$1,msg,info,reload_info){
return (function (){
var s__29828__$1 = s__29828;
while(true){
var temp__5735__auto____$1 = cljs.core.seq(s__29828__$1);
if(temp__5735__auto____$1){
var s__29828__$2 = temp__5735__auto____$1;
if(cljs.core.chunked_seq_QMARK_(s__29828__$2)){
var c__4580__auto__ = cljs.core.chunk_first(s__29828__$2);
var size__4581__auto__ = cljs.core.count(c__4580__auto__);
var b__29830 = cljs.core.chunk_buffer(size__4581__auto__);
if((function (){var i__29829 = (0);
while(true){
if((i__29829 < size__4581__auto__)){
var warning = cljs.core._nth.cljs$core$IFn$_invoke$arity$2(c__4580__auto__,i__29829);
cljs.core.chunk_append(b__29830,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(warning,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100),resource_name));

var G__29917 = (i__29829 + (1));
i__29829 = G__29917;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__29830),shadow$cljs$devtools$client$node$build_complete_$_iter__29825_$_iter__29827(cljs.core.chunk_rest(s__29828__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__29830),null);
}
} else {
var warning = cljs.core.first(s__29828__$2);
return cljs.core.cons(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(warning,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100),resource_name),shadow$cljs$devtools$client$node$build_complete_$_iter__29825_$_iter__29827(cljs.core.rest(s__29828__$2)));
}
} else {
return null;
}
break;
}
});})(s__29826__$1,map__29831,map__29831__$1,src,resource_name,warnings,xs__6292__auto__,temp__5735__auto__,map__29823,map__29823__$1,sources,compiled,map__29820,map__29820__$1,msg,info,reload_info))
,null,null));
});})(s__29826__$1,map__29831,map__29831__$1,src,resource_name,warnings,xs__6292__auto__,temp__5735__auto__,map__29823,map__29823__$1,sources,compiled,map__29820,map__29820__$1,msg,info,reload_info))
;
var fs__4579__auto__ = cljs.core.seq(iterys__4578__auto__(warnings));
if(fs__4579__auto__){
return cljs.core.concat.cljs$core$IFn$_invoke$arity$2(fs__4579__auto__,shadow$cljs$devtools$client$node$build_complete_$_iter__29825(cljs.core.rest(s__29826__$1)));
} else {
var G__29918 = cljs.core.rest(s__29826__$1);
s__29826__$1 = G__29918;
continue;
}
} else {
var G__29919 = cljs.core.rest(s__29826__$1);
s__29826__$1 = G__29919;
continue;
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__4582__auto__(sources);
})()));
if(((shadow.cljs.devtools.client.env.autoload) && (((cljs.core.empty_QMARK_(warnings)) || (shadow.cljs.devtools.client.env.ignore_warnings))))){
var map__29833 = info;
var map__29833__$1 = (((((!((map__29833 == null))))?(((((map__29833.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29833.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29833):map__29833);
var sources__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29833__$1,new cljs.core.Keyword(null,"sources","sources",-321166424));
var compiled__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29833__$1,new cljs.core.Keyword(null,"compiled","compiled",850043082));
var files_to_require = cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"output-name","output-name",-1769107767),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p__29835){
var map__29836 = p__29835;
var map__29836__$1 = (((((!((map__29836 == null))))?(((((map__29836.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29836.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29836):map__29836);
var ns = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29836__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var resource_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29836__$1,new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582));
return ((cljs.core.contains_QMARK_(compiled__$1,resource_id)) || (cljs.core.contains_QMARK_(new cljs.core.Keyword(null,"always-load","always-load",66405637).cljs$core$IFn$_invoke$arity$1(reload_info),ns)));
}),cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p__29838){
var map__29839 = p__29838;
var map__29839__$1 = (((((!((map__29839 == null))))?(((((map__29839.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29839.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29839):map__29839);
var ns = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29839__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
return cljs.core.contains_QMARK_(new cljs.core.Keyword(null,"never-load","never-load",1300896819).cljs$core$IFn$_invoke$arity$1(reload_info),ns);
}),sources__$1))));
if(cljs.core.seq(files_to_require)){
return shadow.cljs.devtools.client.env.do_js_reload.cljs$core$IFn$_invoke$arity$2(msg,(function (){
var seq__29841 = cljs.core.seq(files_to_require);
var chunk__29842 = null;
var count__29843 = (0);
var i__29844 = (0);
while(true){
if((i__29844 < count__29843)){
var src = chunk__29842.cljs$core$IIndexed$_nth$arity$2(null,i__29844);
shadow.cljs.devtools.client.env.before_load_src(src);

shadow.cljs.devtools.client.node.closure_import(src);


var G__29921 = seq__29841;
var G__29922 = chunk__29842;
var G__29923 = count__29843;
var G__29924 = (i__29844 + (1));
seq__29841 = G__29921;
chunk__29842 = G__29922;
count__29843 = G__29923;
i__29844 = G__29924;
continue;
} else {
var temp__5735__auto__ = cljs.core.seq(seq__29841);
if(temp__5735__auto__){
var seq__29841__$1 = temp__5735__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__29841__$1)){
var c__4609__auto__ = cljs.core.chunk_first(seq__29841__$1);
var G__29927 = cljs.core.chunk_rest(seq__29841__$1);
var G__29928 = c__4609__auto__;
var G__29929 = cljs.core.count(c__4609__auto__);
var G__29930 = (0);
seq__29841 = G__29927;
chunk__29842 = G__29928;
count__29843 = G__29929;
i__29844 = G__29930;
continue;
} else {
var src = cljs.core.first(seq__29841__$1);
shadow.cljs.devtools.client.env.before_load_src(src);

shadow.cljs.devtools.client.node.closure_import(src);


var G__29932 = cljs.core.next(seq__29841__$1);
var G__29933 = null;
var G__29934 = (0);
var G__29935 = (0);
seq__29841 = G__29932;
chunk__29842 = G__29933;
count__29843 = G__29934;
i__29844 = G__29935;
continue;
}
} else {
return null;
}
}
break;
}
}));
} else {
return null;
}
} else {
return null;
}
});
shadow.cljs.devtools.client.node.process_message = (function shadow$cljs$devtools$client$node$process_message(p__29846,done){
var map__29847 = p__29846;
var map__29847__$1 = (((((!((map__29847 == null))))?(((((map__29847.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__29847.cljs$core$ISeq$))))?true:false):false))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,map__29847):map__29847);
var msg = map__29847__$1;
var type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__29847__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var G__29849_29936 = type;
var G__29849_29937__$1 = (((G__29849_29936 instanceof cljs.core.Keyword))?G__29849_29936.fqn:null);
switch (G__29849_29937__$1) {
case "repl/init":
shadow.cljs.devtools.client.node.repl_init(msg,done);

break;
case "repl/invoke":
shadow.cljs.devtools.client.node.repl_invoke(msg);

break;
case "repl/set-ns":
shadow.cljs.devtools.client.node.repl_set_ns(msg);

break;
case "repl/require":
shadow.cljs.devtools.client.node.repl_require(msg,done);

break;
case "repl/ping":
shadow.cljs.devtools.client.node.ws_msg(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("repl","pong","repl/pong",-166610159),new cljs.core.Keyword(null,"time-server","time-server",786726561),new cljs.core.Keyword(null,"time-server","time-server",786726561).cljs$core$IFn$_invoke$arity$1(msg),new cljs.core.Keyword(null,"time-runtime","time-runtime",-40294923),Date.now()], null));

break;
case "build-configure":

break;
case "build-start":

break;
case "build-complete":
shadow.cljs.devtools.client.node.build_complete(msg);

break;
case "build-failure":

break;
case "worker-shutdown":
cljs.core.deref(shadow.cljs.devtools.client.node.ws_ref).terminate();

break;
default:
cljs.core.prn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"repl-unknown","repl-unknown",-1898463611),msg], null)], 0));

}

if(cljs.core.contains_QMARK_(shadow.cljs.devtools.client.env.async_ops,type)){
return null;
} else {
return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}
});
shadow.cljs.devtools.client.node.ws_connect = (function shadow$cljs$devtools$client$node$ws_connect(){
var url = shadow.cljs.devtools.client.env.ws_url(new cljs.core.Keyword(null,"node","node",581201198));
var client = (new shadow.js.shim.module$ws(url,cljs.core.PersistentVector.EMPTY));
client.on("open",(function (){
return cljs.core.vreset_BANG_(shadow.cljs.devtools.client.node.ws_ref,client);
}));

client.on("unexpected-response",(function (req,res){
var status = res.statusCode;
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((406),status)){
return console.log("REPL connection rejected, probably stale JS connecting to new server.");
} else {
return console.log("REPL unexpected error",res.statusCode);
}
}));

client.on("message",(function (data,flags){
try{return shadow.cljs.devtools.client.env.process_ws_msg(data,shadow.cljs.devtools.client.node.process_message);
}catch (e29850){var e = e29850;
return console.error("failed to process message",data,e);
}}));

client.on("close",(function (){
return console.log("REPL client disconnected");
}));

return client.on("error",(function (err){
return console.log("REPL client error",err);
}));
});
if(shadow.cljs.devtools.client.env.enabled){
shadow.cljs.devtools.client.node.ws_close();

shadow.cljs.devtools.client.node.ws_connect();
} else {
}

//# sourceMappingURL=shadow.cljs.devtools.client.node.js.map
