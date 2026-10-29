import{i as e,s as t}from"./preload-helper-BdFrVu1K.js";import{t as n}from"./react-mQIxQyHw.js";import{t as r}from"./jsx-runtime-f3rHp9ZU.js";function i(e,t){if(t===`raw`)return{value:e,parseError:null};if(t===`csv`)try{return{value:e.split(`,`).map(e=>e.trim()).filter(Boolean),parseError:null}}catch(e){return{value:null,parseError:e instanceof Error?e.message:`csv parser threw`}}if(t===`urlencoded`)try{let t=new URLSearchParams(e),n={};return t.forEach((e,t)=>{n[t]=e}),{value:n,parseError:null}}catch(e){return{value:null,parseError:e instanceof Error?e.message:`urlencoded parser threw`}}if(t===`auto`&&!l.test(e))return{value:e,parseError:null};try{return{value:JSON.parse(e),parseError:null}}catch(e){return{value:null,parseError:e instanceof Error?e.message:`JSON.parse failed`}}}function a({slotName:e,rawValue:t,parseMode:n}){let{value:r,parseError:a}=(0,s.useMemo)(()=>i(t,n),[t,n]),l=t.length===0,u=l?null:t,d=l?null:r,f=d===null?`null`:typeof d==`string`?JSON.stringify(d):JSON.stringify(d,null,2),p=d===null?`null`:Array.isArray(d)?`${typeof d} (array, ${d.length} item${d.length===1?``:`s`})`:typeof d==`object`?`${typeof d} (record, ${Object.keys(d).length} key${Object.keys(d).length===1?``:`s`})`:typeof d;return(0,c.jsxs)(`div`,{style:{fontFamily:`system-ui, sans-serif`,maxWidth:`640px`},children:[(0,c.jsxs)(`div`,{style:{padding:`12px 14px`,background:`#f8fafc`,borderRadius:`8px`,fontSize:`12px`,color:`#475569`,marginBottom:`12px`,fontFamily:`monospace`},children:[(0,c.jsx)(`strong`,{children:`useTuringSlot<T>(`}),(0,c.jsxs)(`code`,{style:{color:`#0f172a`},children:[`"`,e,`"`]}),n!==`auto`&&(0,c.jsxs)(c.Fragment,{children:[(0,c.jsxs)(`strong`,{children:[`, `,`{ parse: `]}),(0,c.jsx)(`code`,{style:{color:`#0f172a`},children:n===`csv`||n===`urlencoded`?`(raw) => /* ${n} */`:`"${n}"`}),(0,c.jsx)(`strong`,{children:` }`})]}),(0,c.jsx)(`strong`,{children:`)`})]}),(0,c.jsxs)(`div`,{style:{padding:`10px 14px`,background:`#eff6ff`,borderLeft:`3px solid #2563eb`,borderRadius:`0 6px 6px 0`,fontSize:`12px`,color:`#1e3a8a`,marginBottom:`12px`},children:[n===`auto`&&(0,c.jsxs)(c.Fragment,{children:[(0,c.jsx)(`strong`,{children:`auto mode`}),` — runs `,(0,c.jsx)(`code`,{children:`JSON.parse`}),` only when raw matches`,` `,(0,c.jsxs)(`code`,{children:[`/^[[`,`{`,`]/`]}),`. Plain strings pass through untouched.`]}),n===`json`&&(0,c.jsxs)(c.Fragment,{children:[(0,c.jsx)(`strong`,{children:`json mode`}),` — always runs `,(0,c.jsx)(`code`,{children:`JSON.parse`}),`. Plain strings without quotes will surface a `,(0,c.jsx)(`code`,{children:`parseError`}),`.`]}),n===`raw`&&(0,c.jsxs)(c.Fragment,{children:[(0,c.jsx)(`strong`,{children:`raw mode`}),` — never parses. Escape hatch for strings that`,` `,(0,c.jsx)(`em`,{children:`look`}),` like JSON but shouldn't be parsed.`]}),n===`csv`&&(0,c.jsxs)(c.Fragment,{children:[(0,c.jsx)(`strong`,{children:`custom fn`}),` — caller-supplied parser. Here: split by`,` `,(0,c.jsx)(`code`,{children:`,`}),`, trim, drop empty entries → `,(0,c.jsx)(`code`,{children:`string[]`}),`.`]}),n===`urlencoded`&&(0,c.jsxs)(c.Fragment,{children:[(0,c.jsx)(`strong`,{children:`custom fn`}),` — caller-supplied parser. Here: parse with`,` `,(0,c.jsx)(`code`,{children:`URLSearchParams`}),` → `,(0,c.jsx)(`code`,{children:`Record<string, string>`}),`.`]})]}),(0,c.jsxs)(`div`,{style:{border:`1px solid #e2e8f0`,borderRadius:`10px`,overflow:`hidden`,marginBottom:`10px`},children:[(0,c.jsx)(`div`,{style:{padding:`8px 14px`,background:`#f1f5f9`,fontSize:`11px`,fontWeight:600,color:`#475569`,textTransform:`uppercase`,letterSpacing:`0.05em`},children:`Hook return`}),(0,c.jsx)(o,{label:`rawValue`,value:u===null?`null`:JSON.stringify(u)}),(0,c.jsx)(o,{label:`value`,value:f,highlight:a?`error`:d===null?`neutral`:`success`}),(0,c.jsx)(o,{label:`typeof value`,value:p}),(0,c.jsx)(o,{label:`parseError`,value:a===null?`null`:JSON.stringify(a),highlight:a?`error`:`neutral`}),(0,c.jsx)(o,{label:`isLoading`,value:`false`})]}),(0,c.jsxs)(`details`,{style:{fontSize:`12px`,color:`#475569`,marginTop:`10px`},children:[(0,c.jsx)(`summary`,{style:{cursor:`pointer`,fontWeight:600},children:`Como o componente consumiria isso`}),(0,c.jsx)(`pre`,{style:{background:`#0f172a`,color:`#e2e8f0`,padding:`12px`,borderRadius:`6px`,fontSize:`11px`,overflowX:`auto`,marginTop:`8px`},children:`const slot = useTuringSlot${n===`auto`?``:`<T>`}("${e}"${n===`auto`?``:`, { parse: ${n===`csv`||n===`urlencoded`?`(raw) => /* ${n} */`:`"${n}"`} }`});

if (slot.isLoading) return <Spinner />;
if (slot.parseError) {
  console.warn("Bad slot:", slot.rawValue, slot.parseError);
  return <Fallback raw={slot.rawValue} />;
}
if (!slot.value) return null;

return <View data={slot.value} />;`})]})]})}function o({label:e,value:t,highlight:n=`neutral`}){return(0,c.jsxs)(`div`,{style:{display:`flex`,justifyContent:`space-between`,gap:`10px`,padding:`8px 14px`,background:n===`success`?`#f0fdf4`:n===`error`?`#fef2f2`:`white`,borderBottom:`1px solid #f1f5f9`,fontSize:`12px`,alignItems:`flex-start`},children:[(0,c.jsx)(`code`,{style:{color:`#475569`,flexShrink:0},children:e}),(0,c.jsx)(`pre`,{style:{margin:0,color:n===`success`?`#166534`:n===`error`?`#991b1b`:`#0f172a`,textAlign:`right`,fontFamily:`monospace`,fontSize:`11px`,whiteSpace:`pre-wrap`,wordBreak:`break-all`,maxWidth:`65%`},children:t})]})}var s,c,l,u,d,f,p,m,h,g,_,v,y;e((()=>{s=t(n()),c=r(),l=/^[[{]/,u={title:`Hooks/useTuringSlot`,component:a,tags:[`autodocs`],parameters:{layout:`centered`,docs:{description:{component:"Single-slot selector with type-safe parsing. Cycle the `parseMode` arg to watch the same `rawValue` flow through each of the 4 modes — `auto` (default), `json`, `raw`, and two custom-fn examples (CSV-list, URL-encoded)."}}},argTypes:{slotName:{description:`Slot name passed to the hook.`,control:{type:`text`}},rawValue:{description:`Raw slot value as it would arrive from the server.`,control:{type:`text`}},parseMode:{description:"Parsing strategy. The 3 built-in modes (`auto`/`json`/`raw`) plus 2 custom-fn examples (`csv` / `urlencoded`) showing how `(raw) => T` works.",control:{type:`select`},options:[`auto`,`json`,`raw`,`csv`,`urlencoded`]}}},d={name:`auto mode · JSON-looking slot (parsed)`,args:{slotName:`programas_match`,rawValue:`[{"nome":"MBA Executivo","area":"Liderança"},{"nome":"CFO Track","area":"Finanças"}]`,parseMode:`auto`}},f={name:`auto mode · plain string (untouched)`,args:{slotName:`name`,rawValue:`Alexandre Oliveira`,parseMode:`auto`}},p={name:`json mode · always parses`,args:{slotName:`career_path`,rawValue:`{"current":"Gerente","next":"Diretor","horizon":"3 anos"}`,parseMode:`json`}},m={name:`json mode · bad input → parseError`,args:{slotName:`career_path`,rawValue:`Diretor de Marketing`,parseMode:`json`}},h={name:`raw mode · string starting with [ (no parse)`,args:{slotName:`status_label`,rawValue:`[Editado pelo consultor]`,parseMode:`raw`}},g={name:`custom fn · comma-separated list → string[]`,args:{slotName:`tags`,rawValue:`executivo, fintech, c-level, lideranca`,parseMode:`csv`}},_={name:`custom fn · URL params → Record<string,string>`,args:{slotName:`utm_params`,rawValue:`utm_source=linkedin&utm_campaign=education-ee-2026&utm_medium=organic`,parseMode:`urlencoded`}},v={name:`empty slot · value & rawValue both null`,args:{slotName:`objetivo`,rawValue:``,parseMode:`auto`}},y=[`AutoMode_JsonSlot`,`AutoMode_PlainString`,`JsonMode_Strict`,`JsonMode_ParseError`,`RawMode_EscapeHatch`,`CustomParser_CSV`,`CustomParser_UrlEncoded`,`EmptySlot`],d.parameters={...d.parameters,docs:{...d.parameters?.docs,source:{originalSource:`{
  name: "auto mode · JSON-looking slot (parsed)",
  args: {
    slotName: "programas_match",
    rawValue: '[{"nome":"MBA Executivo","area":"Liderança"},{"nome":"CFO Track","area":"Finanças"}]',
    parseMode: "auto"
  }
}`,...d.parameters?.docs?.source}}},f.parameters={...f.parameters,docs:{...f.parameters?.docs,source:{originalSource:`{
  name: "auto mode · plain string (untouched)",
  args: {
    slotName: "name",
    rawValue: "Alexandre Oliveira",
    parseMode: "auto"
  }
}`,...f.parameters?.docs?.source}}},p.parameters={...p.parameters,docs:{...p.parameters?.docs,source:{originalSource:`{
  name: "json mode · always parses",
  args: {
    slotName: "career_path",
    rawValue: '{"current":"Gerente","next":"Diretor","horizon":"3 anos"}',
    parseMode: "json"
  }
}`,...p.parameters?.docs?.source}}},m.parameters={...m.parameters,docs:{...m.parameters?.docs,source:{originalSource:`{
  name: "json mode · bad input → parseError",
  args: {
    slotName: "career_path",
    rawValue: "Diretor de Marketing",
    parseMode: "json"
  }
}`,...m.parameters?.docs?.source}}},h.parameters={...h.parameters,docs:{...h.parameters?.docs,source:{originalSource:`{
  name: "raw mode · string starting with [ (no parse)",
  args: {
    slotName: "status_label",
    rawValue: "[Editado pelo consultor]",
    parseMode: "raw"
  }
}`,...h.parameters?.docs?.source}}},g.parameters={...g.parameters,docs:{...g.parameters?.docs,source:{originalSource:`{
  name: "custom fn · comma-separated list → string[]",
  args: {
    slotName: "tags",
    rawValue: "executivo, fintech, c-level, lideranca",
    parseMode: "csv"
  }
}`,...g.parameters?.docs?.source}}},_.parameters={..._.parameters,docs:{..._.parameters?.docs,source:{originalSource:`{
  name: "custom fn · URL params → Record<string,string>",
  args: {
    slotName: "utm_params",
    rawValue: "utm_source=linkedin&utm_campaign=education-ee-2026&utm_medium=organic",
    parseMode: "urlencoded"
  }
}`,..._.parameters?.docs?.source}}},v.parameters={...v.parameters,docs:{...v.parameters?.docs,source:{originalSource:`{
  name: "empty slot · value & rawValue both null",
  args: {
    slotName: "objetivo",
    rawValue: "",
    parseMode: "auto"
  }
}`,...v.parameters?.docs?.source}}}}))();export{d as AutoMode_JsonSlot,f as AutoMode_PlainString,g as CustomParser_CSV,_ as CustomParser_UrlEncoded,v as EmptySlot,m as JsonMode_ParseError,p as JsonMode_Strict,h as RawMode_EscapeHatch,y as __namedExportsOrder,u as default};