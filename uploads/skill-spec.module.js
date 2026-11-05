














function App() {
  return /* @__PURE__ */ React.createElement(
    Page,
    {
      title: "\u8109\u95E8\u6280\u80FD\u4F53\u7CFB\u89C4\u683C",
      lang: "zh-Hans",
      width: "wide",
      summary: "\u56DB\u5206\u6CD5\u6280\u80FD\u7BA1\u7EBF + P0 \u4E5D\u6280\u80FD\u7EC6\u5219 + \u8FDE\u62DB\u7CFB\u7EDF\uFF08\u500D\u7387\u50AC\u8109 / \u6280\u80FD\u5E8F\u5217\u5FEB\u6377\u952E\uFF09\u3002\u5BF9\u9F50 M1 v2\uFF1B\u50AC\u8109\u500D\u7387\u2260\u900F\u652F\u8D1F\u8109\u503C\u3002"
    },
    /* @__PURE__ */ React.createElement(
      TableOfContents,
      {
        items: [
          { label: "\u51FA\u5904\u4E0E\u539F\u5219", id: "lore" },
          { label: "\u56DB\u5206\u6CD5\u6620\u5C04", id: "types" },
          { label: "\u69FD\u4F4D\u4E0E\u91CA\u653E", id: "slots" },
          { label: "\u8FDE\u62DB\u7CFB\u7EDF", id: "combo" },
          { label: "JSON Schema", id: "json" },
          { label: "\u8109\u51B2\u7EC6\u5219", id: "pulse" },
          { label: "\u8109\u9644\u7EC6\u5219", id: "attach" },
          { label: "\u8109\u9635\u7EC6\u5219", id: "array" },
          { label: "\u6570\u503C\u4E0E\u5E73\u8861", id: "balance" },
          { label: "\u7F8E\u672F\u4E0E\u7279\u6548", id: "art" },
          { label: "\u9A8C\u6536", id: "qa" }
        ]
      }
    ),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u51FA\u5904\u4E0E\u539F\u5219", id: "lore" }, /* @__PURE__ */ React.createElement(Callout, { tone: "info", lead: "\u539F\u8457\u53E3\u5F84\uFF08\u300A\u9B41\u62D4\u4E4B\u4E66\u300B\uFF09" }, "\u9F99\u957F\u8001\u6559\u8FF7\u9E9F\uFF1A\u8109\u672F\u5206\u8109\u51B2\u3001\u8109\u9644\u3001\u8109\u9635\u3001\u8109\u517D\u3002\u7130\u795E\u300A\u5341\u4E8C\u8109\u95E8\u300B\u300A\u8109\u672F\u771F\u7ECF\u300B\uFF1A\u611F\u53D7\u5F62\u8109\uFF0C\u4EBA\u8109\u5408\u4E00\uFF0C\u8D2F\u901A\u8109\u95E8\uFF0C\u632F\u8109\u51FA\u51FB\u3002"), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          { text: "\u539F\u8457\uFF1A\u56DB\u5206\u6CD5\u540D\u79F0\u3001\u8109\u5C4F\u969C\u3001\u8109\u51B2\u53EF\u788E\u7269/\u5BF9\u6218\u3002" },
          { text: "\u63A8\u6F14\uFF1A\u5177\u4F53\u6280\u80FD\u540D\u3001cost/CD\u3001\u8FDE\u62DB\u500D\u7387\u4E0E\u5E8F\u5217\u2014\u2014\u5168\u90E8\u53EF\u6539\u3002" },
          { text: "\u8FB9\u754C\uFF1A\u8109\u517D\u4E0D\u8FDB\u6280\u80FD\u69FD\uFF08M1/M2\uFF09\uFF1B\u5996\u4FA0\u62DB\u5F0F\u5C5E M3\uFF0C\u5171\u4EAB\u8109\u503C\u6C60\u3001CD \u5206\u8F68\u3002" },
          {
            text: "P0\uFF1A\u4E0D\u8DB3\u8109\u503C\u76F4\u63A5\u5931\u8D25\uFF1B\u4E0D\u505A\u300C\u900F\u652F\u5230\u8D1F\u503C\u300D\u50AC\u8109\u3002\u8FDE\u62DB\u7684\u300C\u500D\u7387\u50AC\u8109\u300D\u662F\u9884\u4ED8\u66F4\u9AD8 cost \u6362\u66F4\u5F3A\u6548\u679C\uFF0Ccurrent \u4ECD \u22650\u3002"
          }
        ]
      }
    )),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u56DB\u5206\u6CD5\u6620\u5C04", id: "types" }, /* @__PURE__ */ React.createElement(
      Diagram,
      {
        direction: "horizontal",
        nodes: [
          { id: "pulse", label: "\u8109\u51B2", detail: "\u77AC\u65F6\u6253\u51FB", kind: "box" },
          { id: "fu", label: "\u8109\u9644", detail: "\u9644\u7740\u589E\u76CA", kind: "box" },
          { id: "zhen", label: "\u8109\u9635", detail: "\u573A\u57DF/\u9635\u6CD5", kind: "box" },
          { id: "beast", label: "\u8109\u517D", detail: "\u53EC\u5524\u7CFB\u7EDF", kind: "terminal" }
        ],
        edges: [
          { from: "pulse", to: "fu", dashed: true, label: "\u540C\u6C60\u8109\u503C" },
          { from: "fu", to: "zhen", dashed: true },
          { from: "zhen", to: "beast", dashed: true, label: "\u53E6\u6A21\u5757" }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      Rows,
      {
        rows: [
          {
            title: "\u8109\u51B2 pulse",
            detail: "cast: dash | projectile | strike / next_hit",
            status: { label: "P0\xD73", tone: "danger" }
          },
          {
            title: "\u8109\u9644 attach",
            detail: "cast: buff | next_hit",
            status: { label: "P0\xD73", tone: "danger" }
          },
          {
            title: "\u8109\u9635 array",
            detail: "cast: aoe_zone | barrier | bind",
            status: { label: "P0\xD73", tone: "danger" }
          },
          {
            title: "\u8109\u517D beast",
            detail: "\u72EC\u7ACB\u53EC\u5524\u952E \xB7 M2",
            status: { label: "M2", tone: "warning" }
          }
        ]
      }
    )),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u69FD\u4F4D\u4E0E\u91CA\u653E", id: "slots" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "\u6280\u80FD\u69FD", value: "\u7531\u8109\u95E8\u9636\uFF1A1\u21921\uFF0C3\u21922\uFF0C5\u21923\uFF0C8\u21924" },
          { label: "\u88C5\u5907", value: "MaiScreen \u62D6\u62FD \u2192 EquipSkillPayload(slot, skillId)" },
          { label: "\u5355\u6280\u65BD\u6CD5", value: "CastSkillPayload \u53EA\u4F20 slot\uFF1B\u9ED8\u8BA4 charge=1" },
          { label: "\u5355\u6280\u952E\u4F4D", value: "V/B/N/M = \u69FD1\u20134" },
          { label: "\u8FDE\u62DB\u952E\u4F4D", value: "\u53E6\u952E Z/X/C\uFF08\u53EF\u6539\uFF09\u2192 CastComboPayload(comboIndex)" },
          { label: "\u89E3\u9501", value: "\u8FBE\u5230 unlock_gate \u81EA\u52A8\u8FDB\u6C60\uFF1B\u65E0\u6280\u80FD\u70B9\u8D27\u5E01" }
        ]
      }
    )),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u8FDE\u62DB\u7CFB\u7EDF", id: "combo" }, /* @__PURE__ */ React.createElement(Callout, { tone: "success", lead: "\u73A9\u5BB6\u8BC9\u6C42\uFF08\u5DF2\u843D\u5730\u4E3A\u63A8\u6F14\u89C4\u683C\uFF09" }, "\u9762\u677F\u91CC\u53EF\u914D\uFF1A\u2460 \u540C\u4E00\u6280\u80FD\u7684\u5F3A\u5EA6\u500D\u7387\uFF08\u4E00\u500D/\u4E8C\u500D/\u4E09\u500D\uFF0C\u6D88\u8017\u8FD1\u4F3C\u6307\u6570\u6DA8\u3001\u6548\u679C\u66F4\u5F3A\uFF09\uFF1B\u2461 \u6280\u80FD\u5E8F\u5217\uFF08\u5982\u5148\u8109\u95EA\u518D\u8109\u51FB\uFF09\uFF0C\u7ED1\u5230\u72EC\u7ACB\u5FEB\u6377\u952E\u4E00\u952E\u81EA\u52A8\u653E\u5B8C\u3002"), /* @__PURE__ */ React.createElement(
      Diagram,
      {
        direction: "horizontal",
        nodes: [
          { id: "edit", label: "\u8FDE\u62DB\u9762\u677F", detail: "MaiScreen \u9875\u7B7E", kind: "box" },
          { id: "save", label: "\u5B58 Attachment", detail: "combos[]", kind: "box" },
          { id: "key", label: "\u5FEB\u6377\u952E", detail: "Z/X/C", kind: "box" },
          { id: "run", label: "\u670D\u52A1\u7AEF\u6267\u884C", detail: "CastCombo", kind: "terminal" }
        ],
        edges: [
          { from: "edit", to: "save" },
          { from: "save", to: "key" },
          { from: "key", to: "run" }
        ]
      }
    ), /* @__PURE__ */ React.createElement(Section, { heading: "\u4E24\u79CD\u8FDE\u62DB\u6761\u76EE" }, /* @__PURE__ */ React.createElement(Grid, { columns: 2 }, /* @__PURE__ */ React.createElement(Section, { heading: "A \xB7 \u500D\u7387\u50AC\u8109 amplify" }, /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          { text: "\u9009\u4E00\u4E2A\u5DF2\u89E3\u9501\u6280\u80FD + charge \u7B49\u7EA7 1\u20133" },
          { text: "\u4F8B\uFF1A\u8109\u51FB base cost=10 \u2192 \u4E00\u500D 10 / \u4E8C\u500D 25 / \u4E09\u500D 50" },
          { text: "\u6548\u679C\u6309 charge \u653E\u5927\uFF08\u89C1\u8868\uFF09\uFF1B\u4ECD\u8981\u6C42 current \u2265 \u5E94\u4ED8 cost" },
          { text: "\u4E0E M1\u300C\u900F\u652F\u8D1F\u8109\u300D\u65E0\u5173\uFF1A\u4ED8\u5F97\u8D77\u624D\u653E" }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "B \xB7 \u6280\u80FD\u5E8F\u5217 sequence" }, /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          { text: "\u6709\u5E8F\u6B65\u9AA4\uFF1A\u6BCF\u6B65 = skillId + \u53EF\u9009 charge\uFF08\u9ED8\u8BA4 1\uFF09" },
          { text: "\u4F8B\uFF1A\u8109\u95EA(1) \u2192 \u5EF6\u8FDF \u2192 \u8109\u51FB(1 \u6216\u66F4\u9AD8)" },
          { text: "P0 \u6700\u957F 3 \u6B65\uFF1B\u6B65\u95F4\u95F4\u9694\u53EF\u914D" },
          { text: "\u4E2D\u9014\u5931\u8D25\uFF08\u8109\u4E0D\u8DB3/CD/\u6253\u65AD\uFF09\u505C\u6B62\uFF0C\u5DF2\u653E\u6B65\u9AA4\u4E0D\u9000\u8D39" }
        ]
      }
    )))), /* @__PURE__ */ React.createElement(Section, { heading: "\u500D\u7387\u66F2\u7EBF\uFF08\u63A8\u6F14 \xB7 \u4EE5 base_cost / base_effect \u4E3A 1\xD7\uFF09" }, /* @__PURE__ */ React.createElement(
      Rows,
      {
        rows: [
          {
            title: "charge 1",
            detail: "cost\xD71.0 \xB7 effect\xD71.0 \xB7 \u4F8B\u8109\u51FB cost10 \xB7 \u4F24\u5BB3\u4E58 1.3"
          },
          {
            title: "charge 2",
            detail: "cost\xD72.5 \xB7 effect\xD71.75 \xB7 \u4F8B\u8109\u51FB cost25 \xB7 \u4F24\u5BB3\u4E58 \u22482.28"
          },
          {
            title: "charge 3",
            detail: "cost\xD75.0 \xB7 effect\xD72.75 \xB7 \u4F8B\u8109\u51FB cost50 \xB7 \u4F24\u5BB3\u4E58 \u22483.58"
          }
        ]
      }
    ), /* @__PURE__ */ React.createElement(Text, null, "\u901A\u7528\u5F0F\uFF08\u63A8\u6F14\uFF09\uFF1A`cost(c)=round(base_cost\xD7mult_cost[c])`\uFF0C`effect(c)=base_effect\xD7mult_eff[c]`\uFF1B\u8868 `mult_cost=[1,2.5,5]`\uFF0C`mult_eff=[1,1.75,2.75]`\u3002\u53EF\u6574\u8868\u8FDB `config` / datapack\u3002\u6D88\u8017\u6DA8\u5F97\u6BD4\u6548\u679C\u5FEB\uFF0C\u907F\u514D\u9AD8\u500D\u7387\u6C38\u8FDC\u6700\u4F18\u3002"), /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          {
            label: "effect \u4F5C\u7528\u57DF",
            value: "\u4F24\u5BB3/\u6CBB\u7597/\u5438\u6536/\u51CF\u4F24\u6BD4\u4F8B/\u7F13\u6162\u65F6\u957F\u6216\u7B49\u7EA7\uFF08\u62E9\u4E00\u5199\u8FDB\u6280\u80FD allow_charge_stats\uFF09\uFF1B\u4F4D\u79FB\u7C7B distance \u4EC5 \xD7(1+0.25\xD7(c-1))\uFF0C\u907F\u514D\u4E09\u500D\u95EA\u534A\u56FE"
          },
          {
            label: "CD",
            value: "CD \u4E0D\u968F charge \u7F29\u77ED\uFF1B\u53EF\u9009 CD\xD7(1+0.25\xD7(c-1)) \u9632\u5237\uFF08P0 \u9ED8\u8BA4\uFF1ACD \u4E0D\u53D8\uFF09"
          },
          {
            label: "\u89E3\u9501",
            value: "charge2 \u9700 gate\u22653\uFF1Bcharge3 \u9700 gate\u22655\uFF1B\u5E8F\u5217\u7F16\u8F91\u9700 gate\u22653"
          }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u5E8F\u5217\u65F6\u5E8F" }, /* @__PURE__ */ React.createElement(
      Steps,
      {
        steps: [
          {
            title: "\u6309\u4E0B\u8FDE\u62DB\u952E",
            detail: "C\u2192S CastComboPayload(comboIndex)\uFF1B\u82E5\u5DF2\u5728\u5E8F\u5217\u4E2D\u5219\u5FFD\u7565"
          },
          {
            title: "\u9884\u68C0",
            detail: "\u8BA1\u7B97\u6574\u6BB5\u9884\u4F30 cost \u603B\u548C\uFF08\u53EF\u9009\u4E25\u683C\u9884\u6263\uFF09\xB7 P0 \u63A8\u8350\u9010\u6B65\u6263\uFF1A\u6BCF\u6B65\u524D tryConsume \u8BE5\u6B65\u5E94\u4ED8\uFF1B\u4EFB\u4E00\u6B65\u5931\u8D25\u5219\u505C"
          },
          {
            title: "\u9010\u6B65\u91CA\u653E",
            detail: "\u6267\u884C step.skill @ step.charge\uFF1B\u6B65\u95F4 wait = gap_ticks\uFF08\u9ED8\u8BA4 6\uFF09\uFF1Bdash \u7B49\u9700\u7B49\u4F4D\u79FB\u7A97\u53E3\u7ED3\u675F\u518D\u4E0B\u4E00\u6B65"
          },
          {
            title: "\u6253\u65AD",
            detail: "\u53D7\u51FB/\u6B7B\u4EA1/\u5207\u7EF4/\u9A91\u4E58\u975E\u6CD5 \u2192 \u6E05 comboRuntime\uFF1B\u4E0D\u9000\u5DF2\u6263\u8109\u503C"
          }
        ]
      }
    ), /* @__PURE__ */ React.createElement(Callout, { tone: "warning", lead: "\u540C tick \u89C4\u5219\u653E\u5BBD" }, "\u5355\u6280\u4ECD\u7981\u6B62\u540C tick \u53CC\u7ED3\u7B97\uFF1B\u5E8F\u5217\u5141\u8BB8\u8DE8 tick \u8FDE\u7EED\u65BD\u6CD5\u3002\u5E8F\u5217\u8FDB\u884C\u4E2D\u5C4F\u853D\u5355\u6280\u952E\u4E0E\u5176\u5B83\u8FDE\u62DB\u952E\uFF0C\u907F\u514D\u53E0\u653E\u3002")), /* @__PURE__ */ React.createElement(Section, { heading: "\u9762\u677F UX\uFF08MaiScreen \xB7 \u8FDE\u62DB\u9875\u7B7E\uFF09" }, /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          { text: "P0\uFF1A2 \u4E2A\u8FDE\u62DB\u69FD\uFF08\u540E\u53EF\u6269\u5230 4\uFF09\uFF1B\u6BCF\u69FD\u663E\u793A\u7ED1\u5B9A\u952E\u540D" },
          { text: "\u6A21\u5F0F\u5207\u6362\uFF1A\u500D\u7387 / \u5E8F\u5217" },
          { text: "\u500D\u7387\uFF1A\u4E0B\u62C9\u9009\u6280\u80FD + \u6ED1\u6761/\u6309\u94AE\u9009 1\u20133 \u500D\uFF1B\u53F3\u4FA7\u9884\u89C8 cost \u4E0E\u6548\u679C\u6458\u8981" },
          { text: "\u5E8F\u5217\uFF1A\u4ECE\u5DF2\u89E3\u9501\u5E93\u62D6\u5165\u6B65\u9AA4\u6761\uFF1B\u6BCF\u6B65\u53EF\u70B9\u5F00\u8BBE charge\uFF1B\u6700\u591A 3 \u6B65" },
          { text: "\u975E\u6CD5\u7EC4\u5408\u63D0\u793A\uFF1A\u672A\u89E3\u9501 charge\u3001\u6280\u80FD\u672A\u5B66\u4F1A\u3001\u6B65\u9AA4\u7A7A" },
          { text: "\u4FDD\u5B58 \u2192 SaveCombosPayload \u5168\u91CF\u63D0\u4EA4\uFF1B\u670D\u52A1\u7AEF\u6821\u9A8C\u540E\u5199 Attachment" }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u6570\u636E\u4E0E\u7F51\u7EDC" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          {
            label: "Attachment.combos",
            value: "ComboDef[2..4]\uFF1A`{ mode: amplify|sequence, skillId?, charge?, steps?: [{skillId, charge}], gap_ticks? }`"
          },
          {
            label: "Attachment.comboRuntime",
            value: "\u53EF\u9009\uFF1A`{ comboIndex, step, nextTick }` \u8FDB\u884C\u4E2D\u72B6\u6001\uFF1B\u6B7B\u4EA1/\u65AD\u7EBF\u6E05\u7A7A"
          },
          {
            label: "SaveCombosPayload",
            value: "C\u2192S \u5168\u91CF combos[]\uFF1BS \u6821\u9A8C gate/\u89E3\u9501/\u957F\u5EA6\u540E\u5B58\u76D8\u5E76 Sync"
          },
          {
            label: "CastComboPayload",
            value: "C\u2192S comboIndex\uFF1BS \u542F\u52A8\u8FD0\u884C\u65F6"
          },
          {
            label: "\u6280\u80FD JSON \u53EF\u9009",
            value: "`allow_charge: true`\uFF08\u9ED8\u8BA4 true\uFF09\xB7 `charge_effect: damage|heal|absorption|duration|distance` \xB7 \u7981\u7528\u5217\u8868\u5982\u6CBB\u7597\u53EF `max_charge:2`"
          }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u793A\u4F8B\u914D\u7F6E" }, /* @__PURE__ */ React.createElement(
      Rows,
      {
        rows: [
          {
            title: "\u8FDE\u62DB\u69FD0 \xB7 \u4E09\u500D\u8109\u51FB",
            detail: "mode=amplify \xB7 skill=kuiba:pulse_strike \xB7 charge=3 \xB7 cost50 \xB7 \u952E Z"
          },
          {
            title: "\u8FDE\u62DB\u69FD1 \xB7 \u95EA\u51FB",
            detail: "mode=sequence \xB7 steps=[pulse_flash@1, pulse_strike@2] \xB7 gap=8t \xB7 \u952E X \xB7 \u603B\u6D88\u8017\u7EA6 8+25"
          }
        ]
      }
    ))),
    /* @__PURE__ */ React.createElement(Section, { heading: "JSON Schema", id: "json" }, /* @__PURE__ */ React.createElement(Text, null, "\u8DEF\u5F84\uFF1A`data/kuiba/mai_skill/<path>.json`\u3002\u52A0\u8F7D\uFF1A`SimpleJsonResourceReloadListener`\u3002"), /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "id", value: "kuiba:pulse_flash" },
          { label: "category", value: "pulse | attach | array" },
          { label: "unlock_gate", value: "1\u201312" },
          { label: "cost", value: "float \xB7 1\xD7\u57FA\u51C6" },
          { label: "cooldown_ticks", value: "int" },
          { label: "cast", value: "\u89C1\u5404\u6280\u80FD" },
          { label: "power / range / duration_ticks", value: "\u57FA\u51C6\u503C" },
          { label: "cast_params", value: "\u7C7B\u578B\u4E13\u6709" },
          { label: "allow_charge", value: "bool \u9ED8\u8BA4 true" },
          { label: "max_charge", value: "1\u20133 \u9ED8\u8BA4 3" },
          { label: "charge_effect", value: "damage|heal|absorption|duration|distance|mixed" },
          { label: "icon", value: "kuiba:textures/..." }
        ]
      }
    ), /* @__PURE__ */ React.createElement(Callout, { lead: "\u89E3\u91CA\u5668\u7EA6\u5B9A" }, "\u672A\u77E5 cast \u8BB0\u8B66\u544A\u5E76\u8DF3\u8FC7\u3002\u65BD\u6CD5\u5165\u53E3\u7EDF\u4E00 `MaiCast.execute(player, skillId, charge)`\uFF1B\u5355\u69FD\u4E0E\u8FDE\u62DB\u90FD\u8D70\u5B83\u3002")),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u51B2\u7EC6\u5219", id: "pulse" }, /* @__PURE__ */ React.createElement(Text, null, "\u5168\u90E8\u63A8\u6F14\u547D\u540D\u3002\u4E0B\u5217 cost \u4E3A 1\xD7\uFF1B\u8FDE\u62DB\u500D\u7387\u89C1\u4E0A\u8868\u3002"), /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u95EA kuiba:pulse_flash" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "\u5B9A\u4F4D", value: "\u95E81 \u5165\u95E8\u673A\u52A8" },
          { label: "category / cast", value: "pulse / dash" },
          { label: "unlock_gate \xB7 cost/CD", value: "1 \xB7 8 / 40t" },
          { label: "charge_effect", value: "distance\uFF08\u6E29\u548C\uFF09+ collide_damage" },
          { label: "max_charge", value: "3" }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          {
            text: "cast_params\uFF1A`{ distance:4, invuln_ticks:4, knockback:0.4, collide_damage:1.0 }`"
          },
          {
            text: "\u500D\u7387\uFF1Adistance\xD7(1+0.25\xD7(c-1)) \u2192 4 / 5 / 6\uFF1Bcollide_damage\xD7mult_eff"
          },
          { text: "\u89C4\u5219\uFF1A\u9A91\u4E58\u7981\u7528\uFF1B\u6F5C\u884C\u8DDD\u79BB\xD70.5\uFF1B\u649E\u5899\u63D0\u524D\u7ED3\u675F\u4ECD\u6263\u8D39" }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u51FB kuiba:pulse_strike" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "\u5B9A\u4F4D", value: "\u95E82 \u8FD1\u6218\u5F3A\u5316 \xB7 \u500D\u7387\u793A\u4F8B\u4E3B\u89D2" },
          { label: "category / cast", value: "pulse / next_hit" },
          { label: "unlock_gate \xB7 cost/CD", value: "2 \xB7 10 / 100t" },
          { label: "charge_effect", value: "damage\uFF08damage_mult \u4E0E knockback\uFF09" },
          {
            label: "1/2/3\xD7 \u9884\u89C8",
            value: "cost 10/25/50 \xB7 damage_mult 1.3 / 2.28 / 3.58 \xB7 knockback \u540C\u6BD4\xD7mult_eff"
          }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          {
            text: "cast_params\uFF1A`{ damage_mult:1.3, knockback_bonus:0.8, consume_on_miss:false }`"
          },
          { text: "\u89C4\u5219\uFF1A\u7A97\u53E3\u5185\u9996\u6B21\u8FD1\u6218\u7ED3\u7B97\uFF1BPvP \xD70.7 \u5728\u500D\u7387\u4E4B\u540E" }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u5F39 kuiba:pulse_bolt" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "category / cast", value: "pulse / projectile" },
          { label: "unlock_gate \xB7 cost/CD", value: "3 \xB7 12 / 60t" },
          { label: "charge_effect", value: "damage" },
          { label: "1/2/3\xD7", value: "cost 12/30/60 \xB7 damage 5 / 8.75 / 13.75 \u5FC3" }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          {
            text: "cast_params\uFF1A`{ speed:1.6, gravity:0.0, damage:5.0, pierce:0, size:0.35 }` \xB7 \u9AD8\u500D\u7387 size \u5FAE\u589E\u81F3 0.45"
          }
        ]
      }
    ))),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u9644\u7EC6\u5219", id: "attach" }, /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u62A4 kuiba:mai_guard" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "cast", value: "buff \xB7 \u95E81 \xB7 cost12 / 160t" },
          { label: "charge_effect", value: "absorption" },
          { label: "1/2/3\xD7", value: "cost12/30/60 \xB7 \u5438\u6536 4/7/11 \u5FC3\uFF08\u7531 8HP\xD7mult_eff \u53D6\u6574\uFF09" }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          {
            text: "cast_params\uFF1A`{ absorption:8.0, replace_existing:true }`"
          }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u6108 kuiba:mai_mend" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "cast", value: "buff \xB7 \u95E83 \xB7 cost18 / 240t" },
          { label: "charge_effect", value: "heal" },
          { label: "max_charge", value: "2\uFF08\u63A8\u6F14\uFF1A\u9650\u5236\u6218\u4E2D\u6C38\u52A8\u673A\uFF09" },
          { label: "1/2\xD7", value: "cost18/45 \xB7 \u7597 3/5.25 \u5FC3" }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          { text: "cast_params\uFF1A`{ heal:6.0, combat_ok:true }`" }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u9510 kuiba:mai_edge" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "cast", value: "buff \xB7 \u95E85 \xB7 cost14 / 140t" },
          { label: "charge_effect", value: "duration + attack_speed \u5FAE\u8C03" },
          {
            label: "1/2/3\xD7",
            value: "cost14/35/70 \xB7 \u653B\u901F 0.15/0.20/0.24 \xB7 \u65F6\u957F 8s/10s/12s"
          }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          {
            text: 'cast_params\uFF1A`{ attack_speed:0.15, operation:"multiply_base" }`'
          }
        ]
      }
    ))),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u9635\u7EC6\u5219", id: "array" }, /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u7F1A kuiba:mai_bind" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "cast", value: "bind \xB7 \u95E82 \xB7 cost14 / 140t" },
          { label: "charge_effect", value: "duration\uFF08\u653E\u5927\u5668\u6700\u9AD8\u94B3\u5230 3\uFF09" },
          { label: "1/2/3\xD7", value: "cost14/35/70 \xB7 \u7F13\u6162 3s/4s/5s \xB7 amp 2/2/3" }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          {
            text: 'cast_params\uFF1A`{ effect:"minecraft:slowness", amplifier:2, duration:60, require_look_target:true }` \xB7 \u65E0\u76EE\u6807\u4E0D\u6263\u8D39'
          }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u969C kuiba:mai_barrier" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "cast", value: "barrier \xB7 \u95E84 \xB7 cost16 / 200t" },
          { label: "charge_effect", value: "duration\uFF1B\u51CF\u4F24\u6BD4\u4F8B\u94B3\u5236 \u22640.45" },
          {
            label: "1/2/3\xD7",
            value: "cost16/40/80 \xB7 \u51CF\u4F24 30%/38%/45% \xB7 \u65F6\u957F 2s/2.5s/3s"
          }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          {
            text: "cast_params\uFF1A`{ damage_reduction:0.30, block_projectiles:false, radius:0 }`"
          }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u8109\u7206 kuiba:mai_burst" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "cast", value: "aoe_zone \xB7 \u95E85 \xB7 cost22 / 200t" },
          { label: "charge_effect", value: "damage + radius \u5FAE\u589E" },
          {
            label: "1/2/3\xD7",
            value: "cost22/55/110 \xB7 \u4F24\u5BB3 6/10.5/16.5 \u5FC3 \xB7 \u534A\u5F84 3/3.4/3.8"
          }
        ]
      }
    ), /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          {
            text: "cast_params\uFF1A`{ radius:3.0, damage:6.0, knockback:0.6, friendly_fire:false, ticks:8 }`"
          }
        ]
      }
    ))),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u6570\u503C\u4E0E\u5E73\u8861", id: "balance" }, /* @__PURE__ */ React.createElement(
      Properties,
      {
        items: [
          { label: "\u951A", value: "4\u20135 \u95E8 1\xD7 \u6280\u80FD\u4E0D\u8DB3\u4EE5\u79D2\u6740\u540C\u88C5\u73A9\u5BB6" },
          { label: "\u500D\u7387", value: "cost \u6DA8\u5E45 > effect\uFF0C\u4E09\u500D\u662F\u8C6A\u8D4C\u4E0D\u662F\u9ED8\u8BA4\u8F93\u51FA" },
          { label: "\u5E8F\u5217", value: "\u603B\u6D88\u8017=\u5404\u6B65\u4E4B\u548C\uFF1B\u95EA\u2192\u51FB\u662F\u4F4D\u79FB\u6362\u4F4D\uFF0C\u4E0D\u989D\u5916\u6253\u6298" },
          { label: "\u6CBB\u7597", value: "\u8109\u6108 max_charge=2" },
          { label: "PvP", value: "\xD70.7 \u5728 charge \u4E58\u533A\u4E4B\u540E" },
          { label: "\u900F\u652F", value: "P0 \u4ECD\u4E0D\u505A\u8D1F\u8109\uFF1B\u8FDE\u62DB\u4ED8\u4E0D\u8D77\u5C31\u5931\u8D25" }
        ]
      }
    ), /* @__PURE__ */ React.createElement(Grid, { columns: 2 }, /* @__PURE__ */ React.createElement(Section, { heading: "\u63A8\u8350\u5957\u8DEF" }, /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          { text: "\u69FD\u4F4D\uFF1A\u95EA/\u62A4 + \u8F93\u51FA + \u63A7" },
          { text: "\u8FDE\u62DB0\uFF1A\u4E09\u500D\u8109\u51FB\u6216\u4E8C\u500D\u8109\u7206" },
          { text: "\u8FDE\u62DB1\uFF1A\u8109\u95EA\u2192\u8109\u51FB\uFF08\u6E05\u602A\u8D77\u624B\uFF09" }
        ]
      }
    )), /* @__PURE__ */ React.createElement(Section, { heading: "\u786C\u89C4\u5219" }, /* @__PURE__ */ React.createElement(
      BulletList,
      {
        items: [
          { text: "\u5E8F\u5217\u4E2D\u5C4F\u853D\u5176\u5B83\u65BD\u6CD5\u952E" },
          { text: "\u521B\u9020\u5916\u65E0\u6D88\u8017\u65BD\u6CD5" },
          { text: "\u9A91\u4E58\u7981\u7528\u542B dash \u7684\u6B65\u9AA4" },
          { text: "\u65E0\u76EE\u6807 bind \u6B65\u9AA4\u5931\u8D25\u5E76\u4E2D\u65AD\u5E8F\u5217" }
        ]
      }
    ))), /* @__PURE__ */ React.createElement(Section, { heading: "P0 \u901F\u67E5\uFF081\xD7\uFF09" }, /* @__PURE__ */ React.createElement(
      Rows,
      {
        rows: [
          { title: "\u8109\u95EA", detail: "\u95E81 \xB7 8/2s \xB7 dash 4\u683C" },
          { title: "\u8109\u62A4", detail: "\u95E81 \xB7 12/8s \xB7 \u5438\u65364\u5FC3/3s" },
          { title: "\u8109\u51FB", detail: "\u95E82 \xB7 10/5s \xB7 \u8FD1\u6218\xD71.3 \xB7 \u8FDE\u62DB\u793A\u4F8B 10/25/50" },
          { title: "\u8109\u7F1A", detail: "\u95E82 \xB7 14/7s \xB7 \u7F13\u6162III 3s" },
          { title: "\u8109\u5F39", detail: "\u95E83 \xB7 12/3s \xB7 \u5F39\u4F535\u5FC3" },
          { title: "\u8109\u6108", detail: "\u95E83 \xB7 18/12s \xB7 \u75973\u5FC3 \xB7 \u6700\u9AD82\xD7" },
          { title: "\u8109\u969C", detail: "\u95E84 \xB7 16/10s \xB7 \u51CF\u4F2430%/2s" },
          { title: "\u8109\u9510", detail: "\u95E85 \xB7 14/7s \xB7 \u653B\u901F+15%/8s" },
          { title: "\u8109\u7206", detail: "\u95E85 \xB7 22/10s \xB7 \u534A\u5F843 \xB7 6\u5FC3" }
        ]
      }
    ))),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u7F8E\u672F\u4E0E\u7279\u6548", id: "art" }, /* @__PURE__ */ React.createElement(
      Rows,
      {
        rows: [
          {
            title: "9 \u679A\u56FE\u6807",
            detail: "16\xD716 \xB7 \u4E09\u5206\u7C7B\u8272\u76F8",
            status: { label: "P0", tone: "danger" }
          },
          {
            title: "\u500D\u7387\u89C6\u89C9",
            detail: "2\xD7/3\xD7 \u65BD\u6CD5\u65F6\u7C92\u5B50\u5BC6\u5EA6\u4E0E\u5149\u8272\u52A0\u6DF1\uFF1BHUD \u8FDE\u62DB\u69FD\u663E\u793A \xD72/\xD73 \u89D2\u6807",
            status: { label: "P0", tone: "danger" }
          },
          {
            title: "\u5E8F\u5217\u63D0\u793A",
            detail: "\u91CA\u653E\u4E2D\u6B65\u9AA4\u9AD8\u4EAE\u70B9\uFF081/2/3\uFF09\uFF1B\u53EF\u5360\u4F4D",
            status: { label: "P0", tone: "danger" }
          },
          {
            title: "\u97F3\u6548",
            detail: "\u9AD8\u500D\u7387\u989D\u5916\u4F4E\u97F3\u8109\u9707\uFF1B\u53EF\u5360\u4F4D",
            status: { label: "P1", tone: "warning" }
          }
        ]
      }
    )),
    /* @__PURE__ */ React.createElement(Section, { heading: "\u9A8C\u6536", id: "qa" }, /* @__PURE__ */ React.createElement(
      Checklist,
      {
        items: [
          { text: "9 \u4E2A JSON \u53EF\u52A0\u8F7D\uFF1BMaiCast(skill, charge) \u7EDF\u4E00\u5165\u53E3", done: false },
          { text: "\u8109\u51FB 1/2/3\xD7 cost \u4E3A 10/25/50\uFF0C\u6548\u679C\u6309 mult_eff", done: false },
          { text: "\u5E8F\u5217 \u8109\u95EA\u2192\u8109\u51FB \u4E00\u952E\u6309 gap \u91CA\u653E\uFF1B\u4E2D\u9014\u5931\u8D25\u505C\u6B62\u4E0D\u9000\u8D39", done: false },
          { text: "SaveCombos / CastCombo \u670D\u52A1\u7AEF\u6821\u9A8C gate \u4E0E max_charge", done: false },
          { text: "\u8FDE\u62DB\u8FDB\u884C\u4E2D\u5C4F\u853D\u5355\u6280\u952E\u4E0E\u5176\u5B83\u8FDE\u62DB", done: false },
          { text: "\u8109\u6108\u65E0\u6CD5\u8BBE 3\xD7\uFF1Bcharge \u89E3\u9501\u53D7\u95E8\u9650\u5236", done: false },
          { text: "\u4E0E M1 tryConsume \u6253\u901A\uFF1B\u4ED8\u4E0D\u8D77\u76F4\u63A5\u5931\u8D25", done: false }
        ]
      }
    ), /* @__PURE__ */ React.createElement(Divider, null), /* @__PURE__ */ React.createElement(
      Steps,
      {
        steps: [
          {
            title: "\u7A0B\u5E8F",
            detail: "\u5148 MaiCast+charge \u4E58\u533A \u2192 Save/CastCombo \u2192 \u9762\u677F\u9875\u7B7E\uFF1B\u518D\u94FA\u5E8F\u5217 gap"
          },
          {
            title: "\u540C\u6B65",
            detail: "\u672C\u9875\u53D8\u66F4\u540C\u6B65\u6A21\u7EC4\u5F00\u53D1\u5458\uFF1BM1 Attachment \u9700\u8865 combos \u5B57\u6BB5"
          },
          {
            title: "\u8BBE\u8BA1\u4E0B\u4E00\u6B65",
            detail: "\u53EF\u5F00 M2 \u8109\u517D\uFF0C\u6216\u5FAE\u8C03\u500D\u7387\u8868"
          }
        ]
      }
    ))
  );
}
export {
  App as default
};
