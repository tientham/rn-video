/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { ViewProps } from 'react-native';

interface NativeProps extends ViewProps {
  source?: string;
  play?: boolean;
  volume?: number;
  playerWidth?: number;
  playerHeight?: number;
  replay?: boolean;
}

export default codegenNativeComponent<NativeProps>('RnVideo', {
  excludedPlatforms: ['iOS'],
});
