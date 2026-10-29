import{i as e}from"./preload-helper-BdFrVu1K.js";import{t}from"./jsx-runtime-f3rHp9ZU.js";import{l as n,r,s as i}from"./fixtures-DHV_q1Ge.js";import{n as a,t as o}from"./dist-Cw2NC8_Y.js";var s,c,l,u,d,f,p,m,h;e((()=>{o(),r(),s=t(),{fn:c}=__STORYBOOK_MODULE_TEST__,l={title:`UI Components/TuringPagination`,component:a,tags:[`autodocs`],parameters:{layout:`centered`,docs:{description:{component:`Headless pagination component. Renders Turing API pagination items with support for custom item rendering.`}}},argTypes:{onNavigate:{description:`Callback when a pagination link is clicked. Receives the href string.`,action:`navigated`}}},u={args:{items:i,onNavigate:c()}},d={name:`Simple (3 Pages)`,args:{items:n,onNavigate:c()}},f={name:`Styled (Pill Buttons)`,args:{items:i,onNavigate:c(),itemComponent:({item:e,isCurrent:t,isEllipsis:n,onClick:r,label:i})=>{if(n)return(0,s.jsx)(`span`,{style:{display:`inline-flex`,alignItems:`center`,justifyContent:`center`,width:`36px`,height:`36px`,color:`#94a3b8`,fontWeight:500},children:`…`});let a=[`FIRST`,`PREVIOUS`,`NEXT`,`LAST`].includes(e.type);return(0,s.jsx)(`button`,{onClick:r,disabled:t,style:{display:`inline-flex`,alignItems:`center`,justifyContent:`center`,minWidth:`36px`,height:`36px`,padding:`0 10px`,borderRadius:`8px`,border:`none`,fontSize:a?`18px`:`13px`,fontWeight:t?700:500,fontFamily:`monospace`,cursor:t?`default`:`pointer`,background:t?`linear-gradient(135deg, #2563eb, #4f46e5)`:`transparent`,color:t?`white`:`#475569`,transition:`all 0.15s ease`},children:a?{FIRST:`«`,PREVIOUS:`‹`,NEXT:`›`,LAST:`»`}[e.type]:i})}}},p={name:`Dark Theme`,args:{items:i,onNavigate:c(),itemComponent:({item:e,isCurrent:t,isEllipsis:n,onClick:r,label:i})=>{if(n)return(0,s.jsx)(`span`,{style:{color:`#475569`,padding:`0 4px`},children:`…`});let a=[`FIRST`,`PREVIOUS`,`NEXT`,`LAST`].includes(e.type);return(0,s.jsx)(`button`,{onClick:r,disabled:t,style:{display:`inline-flex`,alignItems:`center`,justifyContent:`center`,minWidth:`32px`,height:`32px`,padding:`0 8px`,borderRadius:`6px`,border:t?`1px solid #3b82f6`:`1px solid #1e293b`,fontSize:`12px`,fontWeight:t?700:400,fontFamily:`monospace`,cursor:t?`default`:`pointer`,background:t?`#1e3a5f`:`#0f172a`,color:t?`#60a5fa`:`#94a3b8`},children:a?{FIRST:`⏮`,PREVIOUS:`◀`,NEXT:`▶`,LAST:`⏭`}[e.type]:i})}},decorators:[e=>(0,s.jsx)(`div`,{style:{display:`flex`,gap:`4px`,alignItems:`center`,background:`#020617`,padding:`16px 24px`,borderRadius:`12px`},children:(0,s.jsx)(e,{})})]},m={name:`No Pagination`,args:{items:[],onNavigate:c()}},h=[`Default`,`Simple`,`StyledPagination`,`DarkTheme`,`NoPagination`],u.parameters={...u.parameters,docs:{...u.parameters?.docs,source:{originalSource:`{
  args: {
    items: samplePagination,
    onNavigate: fn()
  }
}`,...u.parameters?.docs?.source},description:{story:'## Default (Unstyled)\n\nWithout `itemComponent`, renders basic `<button>` elements.\nCURRENT page is disabled, ELLIPSIS shows "…".',...u.parameters?.docs?.description}}},d.parameters={...d.parameters,docs:{...d.parameters?.docs,source:{originalSource:`{
  name: "Simple (3 Pages)",
  args: {
    items: simplePagination,
    onNavigate: fn()
  }
}`,...d.parameters?.docs?.source},description:{story:`## Simple (3 pages)

A compact pagination with just 3 pages and a Next button.`,...d.parameters?.docs?.description}}},f.parameters={...f.parameters,docs:{...f.parameters?.docs,source:{originalSource:`{
  name: "Styled (Pill Buttons)",
  args: {
    items: samplePagination,
    onNavigate: fn(),
    itemComponent: ({
      item,
      isCurrent,
      isEllipsis,
      onClick,
      label
    }) => {
      if (isEllipsis) {
        return <span style={{
          display: "inline-flex",
          alignItems: "center",
          justifyContent: "center",
          width: "36px",
          height: "36px",
          color: "#94a3b8",
          fontWeight: 500
        }}>
            …
          </span>;
      }
      const isNav = ["FIRST", "PREVIOUS", "NEXT", "LAST"].includes(item.type);
      const navSymbols: Record<string, string> = {
        FIRST: "«",
        PREVIOUS: "‹",
        NEXT: "›",
        LAST: "»"
      };
      return <button onClick={onClick} disabled={isCurrent} style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        minWidth: "36px",
        height: "36px",
        padding: "0 10px",
        borderRadius: "8px",
        border: "none",
        fontSize: isNav ? "18px" : "13px",
        fontWeight: isCurrent ? 700 : 500,
        fontFamily: "monospace",
        cursor: isCurrent ? "default" : "pointer",
        background: isCurrent ? "linear-gradient(135deg, #2563eb, #4f46e5)" : "transparent",
        color: isCurrent ? "white" : "#475569",
        transition: "all 0.15s ease"
      }}>
          {isNav ? navSymbols[item.type] : label}
        </button>;
    }
  }
}`,...f.parameters?.docs?.source},description:{story:`## Styled Pagination

Custom \`itemComponent\` with pill-style buttons and gradient active state.
This mirrors the style used in the turing-marketplace projects.`,...f.parameters?.docs?.description}}},p.parameters={...p.parameters,docs:{...p.parameters?.docs,source:{originalSource:`{
  name: "Dark Theme",
  args: {
    items: samplePagination,
    onNavigate: fn(),
    itemComponent: ({
      item,
      isCurrent,
      isEllipsis,
      onClick,
      label
    }) => {
      if (isEllipsis) {
        return <span style={{
          color: "#475569",
          padding: "0 4px"
        }}>…</span>;
      }
      const isNav = ["FIRST", "PREVIOUS", "NEXT", "LAST"].includes(item.type);
      const navSymbols: Record<string, string> = {
        FIRST: "⏮",
        PREVIOUS: "◀",
        NEXT: "▶",
        LAST: "⏭"
      };
      return <button onClick={onClick} disabled={isCurrent} style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        minWidth: "32px",
        height: "32px",
        padding: "0 8px",
        borderRadius: "6px",
        border: isCurrent ? "1px solid #3b82f6" : "1px solid #1e293b",
        fontSize: "12px",
        fontWeight: isCurrent ? 700 : 400,
        fontFamily: "monospace",
        cursor: isCurrent ? "default" : "pointer",
        background: isCurrent ? "#1e3a5f" : "#0f172a",
        color: isCurrent ? "#60a5fa" : "#94a3b8"
      }}>
          {isNav ? navSymbols[item.type] : label}
        </button>;
    }
  },
  decorators: [Story => <div style={{
    display: "flex",
    gap: "4px",
    alignItems: "center",
    background: "#020617",
    padding: "16px 24px",
    borderRadius: "12px"
  }}>
        <Story />
      </div>]
}`,...p.parameters?.docs?.source},description:{story:`## Dark Theme

Pagination styled for dark backgrounds, as used in the Space Missions example.`,...p.parameters?.docs?.description}}},m.parameters={...m.parameters,docs:{...m.parameters?.docs,source:{originalSource:`{
  name: "No Pagination",
  args: {
    items: [],
    onNavigate: fn()
  }
}`,...m.parameters?.docs?.source},description:{story:`## No Pagination

When no items are provided, nothing is rendered.`,...m.parameters?.docs?.description}}}}))();export{p as DarkTheme,u as Default,m as NoPagination,d as Simple,f as StyledPagination,h as __namedExportsOrder,l as default};