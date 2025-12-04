/**
 * 国际化配置
 * Internationalization Configuration
 */

import zhCN from './zh-CN'

// 当前语言
export const currentLocale = 'zh-CN'

// 语言包映射
export const locales = {
  'zh-CN': zhCN,
}

// 获取当前语言包
export const getLocale = () => {
  return locales[currentLocale as keyof typeof locales]
}

// 获取翻译文本
export const t = (key: string, params?: Record<string, any>): string => {
  const locale = getLocale()
  const keys = key.split('.')
  
  let value: any = locale
  for (const k of keys) {
    if (value && typeof value === 'object' && k in value) {
      value = value[k]
    } else {
      return key // 如果找不到翻译，返回 key
    }
  }
  
  // 如果有参数，进行替换
  if (params && typeof value === 'string') {
    return value.replace(/\{(\w+)\}/g, (match, paramKey) => {
      return params[paramKey] !== undefined ? String(params[paramKey]) : match
    })
  }
  
  return typeof value === 'string' ? value : key
}

export default {
  currentLocale,
  locales,
  getLocale,
  t,
}
