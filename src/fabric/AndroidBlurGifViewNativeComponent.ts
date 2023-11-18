/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { ViewProps } from 'react-native';

interface NativeProps extends ViewProps {
  source?: string;
}

export default codegenNativeComponent<NativeProps>('RnVideo-3sbg', {
  excludedPlatforms: ['iOS'],
});
