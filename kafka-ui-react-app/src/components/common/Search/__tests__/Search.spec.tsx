import Search from 'components/common/Search/Search';
import React from 'react';
import { render } from 'lib/testHelpers';
import userEvent from '@testing-library/user-event';

jest.mock('use-debounce', () => ({
  useDebouncedCallback: (fn: (e: Event) => void) => fn,
}));

describe('Search', () => {
  const handleSearch = jest.fn();
  it('calls handleSearch on input', () => {
    const component = render(
      <Search
        handleSearch={handleSearch}
        value=""
        placeholder="Search bt the Topic name"
      />
    );
    const input = component.baseElement.querySelector('input') as HTMLElement;
    userEvent.click(input);
    userEvent.keyboard('value');
    expect(handleSearch).toHaveBeenCalledTimes(5);
  });

  describe('when placeholder is provided', () => {
    it('matches the snapshot', () => {
      const component = render(
        <Search
          handleSearch={handleSearch}
          value=""
          placeholder="Search bt the Topic name"
        />
      );
      expect(component.baseElement).toMatchSnapshot();
    });
  });

  describe('when placeholder is not provided', () => {
    it('matches the snapshot', () => {
      const component = render(<Search handleSearch={handleSearch} value="" />);
      expect(component.baseElement).toMatchSnapshot();
    });
  });
});
