import{i as e}from"./preload-helper-BdFrVu1K.js";import{t}from"./jsx-runtime-f3rHp9ZU.js";import{o as n,t as r}from"./dist-Cw2NC8_Y.js";var i,a,o,s,c,l,u,d,f;e((()=>{r(),i=t(),{fn:a}=__STORYBOOK_MODULE_TEST__,o={title:`UI Components/TuringSearchBar`,component:n,tags:[`autodocs`],parameters:{layout:`centered`,docs:{description:{component:`Simple search bar with customizable input/button. Uses render props for full control over the appearance.`}}},argTypes:{onSearch:{description:`Callback fired when the user submits a search`,action:`searched`},placeholder:{description:`Placeholder text for the input`,control:`text`},defaultQuery:{description:`Pre-fill the input with a default query`,control:`text`}}},s={args:{onSearch:a(),placeholder:`Search...`}},c={name:`Styled Input`,args:{onSearch:a(),placeholder:`Search products...`,renderInput:e=>(0,i.jsx)(`input`,{...e,style:{padding:`10px 16px`,fontSize:`16px`,border:`2px solid #6366f1`,borderRadius:`8px`,outline:`none`,width:`300px`}})}},l={name:`Custom Button`,args:{onSearch:a(),placeholder:`Search creatures...`,renderInput:e=>(0,i.jsx)(`input`,{...e,style:{padding:`10px 16px`,fontSize:`14px`,border:`1px solid #e2e8f0`,borderRadius:`8px 0 0 8px`,outline:`none`,width:`260px`}}),renderButton:({onClick:e})=>(0,i.jsx)(`button`,{onClick:e,style:{padding:`10px 20px`,fontSize:`14px`,background:`linear-gradient(135deg, #2563eb, #4f46e5)`,color:`white`,border:`none`,borderRadius:`0 8px 8px 0`,cursor:`pointer`,fontWeight:600},children:`🔍 Search`})}},u={name:`Pre-filled Query`,args:{onSearch:a(),defaultQuery:`dragon`,placeholder:`Search...`}},d={name:`Card Style`,args:{onSearch:a(),placeholder:`What are you looking for?`},render:e=>(0,i.jsxs)(`div`,{style:{padding:`24px`,background:`white`,borderRadius:`16px`,boxShadow:`0 4px 24px rgba(0,0,0,0.08)`,maxWidth:`480px`},children:[(0,i.jsx)(`h3`,{style:{margin:`0 0 12px`,fontSize:`18px`,fontWeight:700},children:`🔍 Enterprise Search`}),(0,i.jsx)(n,{...e,className:`search-bar`,renderInput:e=>(0,i.jsx)(`input`,{...e,style:{width:`100%`,padding:`12px 16px`,fontSize:`15px`,border:`2px solid #e2e8f0`,borderRadius:`10px`,outline:`none`,marginBottom:`8px`,boxSizing:`border-box`}}),renderButton:({onClick:e})=>(0,i.jsx)(`button`,{onClick:e,style:{width:`100%`,padding:`10px`,fontSize:`14px`,fontWeight:600,background:`linear-gradient(135deg, #2563eb, #4f46e5)`,color:`white`,border:`none`,borderRadius:`10px`,cursor:`pointer`},children:`Search`})})]})},f=[`Default`,`StyledInput`,`CustomButton`,`PrefilledQuery`,`CardStyle`],s.parameters={...s.parameters,docs:{...s.parameters?.docs,source:{originalSource:`{
  args: {
    onSearch: fn(),
    placeholder: "Search..."
  }
}`,...s.parameters?.docs?.source},description:{story:"## Default (Unstyled)\n\nWithout render props, the component renders a plain `<input>` and `<button>`.\nThis is the simplest usage — just provide `onSearch`.",...s.parameters?.docs?.description}}},c.parameters={...c.parameters,docs:{...c.parameters?.docs,source:{originalSource:`{
  name: "Styled Input",
  args: {
    onSearch: fn(),
    placeholder: "Search products...",
    renderInput: props => <input {...props} style={{
      padding: "10px 16px",
      fontSize: "16px",
      border: "2px solid #6366f1",
      borderRadius: "8px",
      outline: "none",
      width: "300px"
    }} />
  }
}`,...c.parameters?.docs?.source},description:{story:`## With Custom Styled Input

Use \`renderInput\` to fully control the input element's appearance.
The render prop receives all necessary props (value, onChange, ref, etc.)`,...c.parameters?.docs?.description}}},l.parameters={...l.parameters,docs:{...l.parameters?.docs,source:{originalSource:`{
  name: "Custom Button",
  args: {
    onSearch: fn(),
    placeholder: "Search creatures...",
    renderInput: props => <input {...props} style={{
      padding: "10px 16px",
      fontSize: "14px",
      border: "1px solid #e2e8f0",
      borderRadius: "8px 0 0 8px",
      outline: "none",
      width: "260px"
    }} />,
    renderButton: ({
      onClick
    }) => <button onClick={onClick} style={{
      padding: "10px 20px",
      fontSize: "14px",
      background: "linear-gradient(135deg, #2563eb, #4f46e5)",
      color: "white",
      border: "none",
      borderRadius: "0 8px 8px 0",
      cursor: "pointer",
      fontWeight: 600
    }}>
        🔍 Search
      </button>
  }
}`,...l.parameters?.docs?.source},description:{story:"## With Custom Button\n\nUse `renderButton` to replace the default submit button.\nThe render prop receives `{ onClick }` to trigger the search.",...l.parameters?.docs?.description}}},u.parameters={...u.parameters,docs:{...u.parameters?.docs,source:{originalSource:`{
  name: "Pre-filled Query",
  args: {
    onSearch: fn(),
    defaultQuery: "dragon",
    placeholder: "Search..."
  }
}`,...u.parameters?.docs?.source},description:{story:`## Pre-filled Query

Pass \`defaultQuery\` to pre-fill the search input.
Useful when navigating back to a search page with a saved query.`,...u.parameters?.docs?.description}}},d.parameters={...d.parameters,docs:{...d.parameters?.docs,source:{originalSource:`{
  name: "Card Style",
  args: {
    onSearch: fn(),
    placeholder: "What are you looking for?"
  },
  render: args => <div style={{
    padding: "24px",
    background: "white",
    borderRadius: "16px",
    boxShadow: "0 4px 24px rgba(0,0,0,0.08)",
    maxWidth: "480px"
  }}>
      <h3 style={{
      margin: "0 0 12px",
      fontSize: "18px",
      fontWeight: 700
    }}>
        🔍 Enterprise Search
      </h3>
      <TuringSearchBar {...args} className="search-bar" renderInput={props => <input {...props} style={{
      width: "100%",
      padding: "12px 16px",
      fontSize: "15px",
      border: "2px solid #e2e8f0",
      borderRadius: "10px",
      outline: "none",
      marginBottom: "8px",
      boxSizing: "border-box"
    }} />} renderButton={({
      onClick
    }) => <button onClick={onClick} style={{
      width: "100%",
      padding: "10px",
      fontSize: "14px",
      fontWeight: 600,
      background: "linear-gradient(135deg, #2563eb, #4f46e5)",
      color: "white",
      border: "none",
      borderRadius: "10px",
      cursor: "pointer"
    }}>
            Search
          </button>} />
    </div>
}`,...d.parameters?.docs?.source},description:{story:`## Full Example (Card Style)

A more complete example showing the search bar in a card layout,
similar to what you'd see in a real application header.`,...d.parameters?.docs?.description}}}}))();export{d as CardStyle,l as CustomButton,s as Default,u as PrefilledQuery,c as StyledInput,f as __namedExportsOrder,o as default};