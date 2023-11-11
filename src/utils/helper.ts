/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

import { Platform } from 'react-native';

export function isIos(): boolean {
  return Platform.OS === 'ios';
}

export function isAndroid(): boolean {
  return Platform.OS === 'android';
}
