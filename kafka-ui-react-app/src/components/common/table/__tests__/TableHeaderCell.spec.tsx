import React from 'react';
import { screen, within } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import TableHeaderCell, {
  TableHeaderCellProps,
} from 'components/common/table/TableHeaderCell/TableHeaderCell';
import { TopicColumnsToSort } from 'generated-sources';
import theme from 'theme/theme';
import userEvent from '@testing-library/user-event';

const title = 'test title';
const previewText = 'test preview text';
const orderBy = TopicColumnsToSort.NAME;
const orderValue = TopicColumnsToSort.NAME;
const otherOrderValue = TopicColumnsToSort.OUT_OF_SYNC_REPLICAS;
const handleOrderBy = jest.fn();
const sortIconTitle = 'Sort icon';
const onPreview = jest.fn();
const SPACE_KEY = ' ';

describe('TableHeaderCell', () => {
  const setupComponent = (props: Partial<TableHeaderCellProps> = {}) =>
    render(
      <table>
        <thead>
          <tr>
            <TableHeaderCell {...props} />
          </tr>
        </thead>
      </table>
    );

  it('renders without props', () => {
    setupComponent();
    expect(screen.getByRole('columnheader')).toBeInTheDocument();
  });

  it('renders with title & preview text', () => {
    setupComponent({
      title,
      previewText,
    });

    const th = screen.getByRole('columnheader');
    expect(within(th).getByText(title)).toBeInTheDocument();
    expect(within(th).getByText(previewText)).toBeInTheDocument();
  });

  it('renders with orderable props', () => {
    setupComponent({
      title,
      orderBy,
      orderValue,
      handleOrderBy,
    });
    const th = screen.getByRole('columnheader');
    const titleNode = within(th).getByRole('button');
    expect(titleNode).toBeInTheDocument();
    expect(titleNode).toHaveTextContent(title);
    expect(within(titleNode).getByTitle(sortIconTitle)).toBeInTheDocument();
    expect(titleNode).toHaveStyle(`color: ${theme.thStyles.color.active};`);
    expect(titleNode).toHaveStyle('cursor: pointer;');
  });

  it('renders click on title triggers handler', () => {
    setupComponent({
      title,
      orderBy,
      orderValue,
      handleOrderBy,
    });
    const th = screen.getByRole('columnheader');
    const titleNode = within(th).getByRole('button');
    userEvent.click(titleNode);
    expect(handleOrderBy.mock.calls.length).toBe(1);
  });

  it('renders space on title triggers handler', () => {
    setupComponent({
      title,
      orderBy,
      orderValue,
      handleOrderBy,
    });
    const th = screen.getByRole('columnheader');
    const titleNode = within(th).getByRole('button');
    userEvent.type(titleNode, SPACE_KEY);
    // userEvent.type clicks and only then presses space
    expect(handleOrderBy.mock.calls.length).toBe(2);
  });

  it('click on preview triggers handler', () => {
    setupComponent({
      title,
      previewText,
      onPreview,
    });
    const th = screen.getByRole('columnheader');
    const previewNode = within(th).getByRole('button');
    userEvent.click(previewNode);
    expect(onPreview.mock.calls.length).toBe(1);
  });

  it('click on preview triggers handler', () => {
    setupComponent({
      title,
      previewText,
      onPreview,
    });
    const th = screen.getByRole('columnheader');
    const previewNode = within(th).getByRole('button');
    userEvent.type(previewNode, SPACE_KEY);
    // userEvent.type clicks and only then presses space
    expect(onPreview.mock.calls.length).toBe(2);
  });

  it('renders without sort indication', () => {
    setupComponent({
      title,
      orderBy,
    });

    const th = screen.getByRole('columnheader');
    const titleNode = within(th).getByText(title);
    expect(
      within(titleNode).queryByTitle(sortIconTitle)
    ).not.toBeInTheDocument();
    expect(titleNode).toHaveStyle('cursor: default;');
  });

  it('renders with hightlighted title when orderBy and orderValue are equal', () => {
    setupComponent({
      title,
      orderBy,
      orderValue,
    });
    const th = screen.getByRole('columnheader');
    const titleNode = within(th).getByText(title);
    expect(titleNode).toHaveStyle(`color: ${theme.thStyles.color.active};`);
  });

  it('renders without hightlighted title when orderBy and orderValue are not equal', () => {
    setupComponent({
      title,
      orderBy,
      orderValue: otherOrderValue,
    });
    const th = screen.getByRole('columnheader');
    const titleNode = within(th).getByText(title);
    expect(titleNode).toHaveStyle(`color: ${theme.thStyles.color.normal}`);
  });

  it('renders with default (primary) theme', () => {
    setupComponent({
      title,
    });

    const th = screen.getByRole('columnheader');
    const titleNode = within(th).getByText(title);
    expect(titleNode).toHaveStyle(
      `background: ${theme.thStyles.backgroundColor.normal};`
    );
  });
});
