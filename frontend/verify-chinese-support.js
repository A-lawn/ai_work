/**
 * 中文支持验证脚本
 * Chinese Support Verification Script
 */

const fs = require('fs');
const path = require('path');

console.log('='.repeat(60));
console.log('中文界面支持验证');
console.log('Chinese Language Support Verification');
console.log('='.repeat(60));
console.log('');

// 1. 检查 HTML 文件的 UTF-8 编码和语言设置
console.log('1. 检查 HTML 配置...');
const htmlPath = path.join(__dirname, 'index.html');
const htmlContent = fs.readFileSync(htmlPath, 'utf8');

if (htmlContent.includes('charset="UTF-8"')) {
  console.log('   ✓ UTF-8 编码已配置');
} else {
  console.log('   ✗ UTF-8 编码未配置');
}

if (htmlContent.includes('lang="zh-CN"')) {
  console.log('   ✓ 中文语言标签已设置');
} else {
  console.log('   ✗ 中文语言标签未设置');
}

if (htmlContent.includes('智能运维问答助手')) {
  console.log('   ✓ 中文标题已设置');
} else {
  console.log('   ✗ 中文标题未设置');
}

console.log('');

// 2. 检查 main.tsx 的 Day.js 中文配置
console.log('2. 检查 Day.js 中文配置...');
const mainPath = path.join(__dirname, 'src', 'main.tsx');
const mainContent = fs.readFileSync(mainPath, 'utf8');

if (mainContent.includes("import 'dayjs/locale/zh-cn'")) {
  console.log('   ✓ Day.js 中文语言包已导入');
} else {
  console.log('   ✗ Day.js 中文语言包未导入');
}

if (mainContent.includes("dayjs.locale('zh-cn')")) {
  console.log('   ✓ Day.js 中文语言已设置');
} else {
  console.log('   ✗ Day.js 中文语言未设置');
}

console.log('');

// 3. 检查 App.tsx 的 Ant Design 中文配置
console.log('3. 检查 Ant Design 中文配置...');
const appPath = path.join(__dirname, 'src', 'App.tsx');
const appContent = fs.readFileSync(appPath, 'utf8');

if (appContent.includes("import zhCN from 'antd/locale/zh_CN'")) {
  console.log('   ✓ Ant Design 中文语言包已导入');
} else {
  console.log('   ✗ Ant Design 中文语言包未导入');
}

if (appContent.includes('locale={zhCN}')) {
  console.log('   ✓ Ant Design 中文语言已配置');
} else {
  console.log('   ✗ Ant Design 中文语言未配置');
}

if (appContent.includes('智能运维问答助手')) {
  console.log('   ✓ 应用包含中文内容');
} else {
  console.log('   ✗ 应用不包含中文内容');
}

console.log('');

// 4. 检查中文语言包文件
console.log('4. 检查中文语言包...');
const localePath = path.join(__dirname, 'src', 'locales', 'zh-CN.ts');
if (fs.existsSync(localePath)) {
  console.log('   ✓ 中文语言包文件存在');
  const localeContent = fs.readFileSync(localePath, 'utf8');
  
  const categories = [
    'common', 'menu', 'document', 'query', 'session', 
    'config', 'monitor', 'error', 'validation'
  ];
  
  let foundCategories = 0;
  categories.forEach(cat => {
    if (localeContent.includes(`${cat}:`)) {
      foundCategories++;
    }
  });
  
  console.log(`   ✓ 找到 ${foundCategories}/${categories.length} 个翻译类别`);
} else {
  console.log('   ✗ 中文语言包文件不存在');
}

console.log('');

// 5. 检查国际化工具函数
console.log('5. 检查国际化工具...');
const i18nPath = path.join(__dirname, 'src', 'locales', 'index.ts');
if (fs.existsSync(i18nPath)) {
  console.log('   ✓ 国际化工具文件存在');
  const i18nContent = fs.readFileSync(i18nPath, 'utf8');
  
  if (i18nContent.includes('export const t =')) {
    console.log('   ✓ 翻译函数已导出');
  } else {
    console.log('   ✗ 翻译函数未导出');
  }
  
  if (i18nContent.includes("currentLocale = 'zh-CN'")) {
    console.log('   ✓ 默认语言设置为中文');
  } else {
    console.log('   ✗ 默认语言未设置为中文');
  }
} else {
  console.log('   ✗ 国际化工具文件不存在');
}

console.log('');

// 6. 检查构建输出
console.log('6. 检查构建配置...');
const distPath = path.join(__dirname, 'dist');
if (fs.existsSync(distPath)) {
  console.log('   ✓ 构建输出目录存在');
  
  const distHtmlPath = path.join(distPath, 'index.html');
  if (fs.existsSync(distHtmlPath)) {
    console.log('   ✓ 构建的 HTML 文件存在');
    const distHtmlContent = fs.readFileSync(distHtmlPath, 'utf8');
    
    if (distHtmlContent.includes('charset="UTF-8"')) {
      console.log('   ✓ 构建文件保持 UTF-8 编码');
    }
    
    if (distHtmlContent.includes('智能运维问答助手')) {
      console.log('   ✓ 构建文件包含中文内容');
    }
  } else {
    console.log('   ℹ 构建的 HTML 文件不存在（需要先运行 npm run build）');
  }
} else {
  console.log('   ℹ 构建输出目录不存在（需要先运行 npm run build）');
}

console.log('');
console.log('='.repeat(60));
console.log('验证完成！');
console.log('Verification Complete!');
console.log('='.repeat(60));
console.log('');
console.log('如需查看实际效果，请运行：');
console.log('To see the actual effect, run:');
console.log('  npm run dev');
console.log('');
console.log('然后访问 http://localhost:5173');
console.log('Then visit http://localhost:5173');
console.log('');
