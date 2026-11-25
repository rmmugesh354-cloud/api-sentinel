import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatCard } from '../components/StatCard';
import { Activity } from 'lucide-react';

describe('StatCard', () => {
  it('renders title, value, and subtext correctly', () => {
    render(
      <StatCard
        title="Active Endpoints"
        value={15}
        subtext="12 healthy, 3 paused"
        icon={Activity}
        variant="cyan"
      />
    );

    expect(screen.getByText('Active Endpoints')).toBeInTheDocument();
    expect(screen.getByText('15')).toBeInTheDocument();
    expect(screen.getByText('12 healthy, 3 paused')).toBeInTheDocument();
  });
});
