import { CellContext } from '@tanstack/react-table';
import { formatTimestamp } from 'lib/dateTimeHelpers';
import React from 'react';

import * as S from './Table.styled';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const TimestampCell: React.FC<CellContext<any, any>> = ({ getValue }) => (
  <S.Nowrap>{formatTimestamp(getValue())}</S.Nowrap>
);

export default TimestampCell;
