import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatusBadge } from '../components/StatusBadge';

describe('StatusBadge', () => {
  it('renders HEALTHY status correctly', () => {
    render(<StatusBadge status="HEALTHY" />);
    expect(screen.getByText('HEALTHY')).toBeInTheDocument();
  });

  it('renders UNHEALTHY status with alert styling', () => {
    render(<StatusBadge status="UNHEALTHY" />);
    const badge = screen.getByText('UNHEALTHY');
    expect(badge).toBeInTheDocument();
  });

  it('renders DEGRADED status correctly', () => {
    render(<StatusBadge status="DEGRADED" />);
    expect(screen.getByText('DEGRADED')).toBeInTheDocument();
  });

  it('renders PAUSED status correctly', () => {
    render(<StatusBadge status="PAUSED" />);
    expect(screen.getByText('PAUSED')).toBeInTheDocument();
  });
});
