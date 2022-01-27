import React from 'react';

import { CustomButtonIconContainer } from './CustomButtonIcons.styled';

const ButtonPlusIcon: React.FC = () => {
  return (
    <CustomButtonIconContainer>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 16 16"
        fill="none"
      >
        <path
          d="M7 3.94446C7 3.39217 7.44772 2.94446 8 2.94446C8.55228 2.94446 9 3.39217 9 3.94446V11.9445C9 12.4967 8.55228 12.9445 8 12.9445C7.44772 12.9445 7 12.4967 7 11.9445V3.94446Z"
          fill="#171A1C"
        />
        <path
          d="M12 9.00006C12.5523 9.00006 13 8.55235 13 8.00006C13 7.44778 12.5523 7.00006 12 7.00006H4C3.44772 7.00006 3 7.44778 3 8.00006C3 8.55235 3.44772 9.00006 4 9.00006H12Z"
          fill="#171A1C"
        />
      </svg>
    </CustomButtonIconContainer>
  );
};

export default ButtonPlusIcon;
