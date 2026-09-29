import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ScreeningService } from './screening.service';
import { BatchDetail, BatchListItem } from './screening.model';

interface Segment {
  verdict: string;
  label: string;
  color: string;
  count: number;
  pct: number;
}

/**
 * Sales-facing dashboard for a screening batch: KPI tiles, a verdict-composition
 * bar, and the flagged table. Verdict colours are a validated severity palette
 * (novel → possible-overlap → likely-duplicate) always shown WITH the label, so
 * identity never rests on colour alone. (Host app is light-themed.)
 */
@Component({
  selector: 'app-screening-dashboard',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './screening-dashboard.component.html',
  styleUrl: './screening-dashboard.component.scss',
})
export class ScreeningDashboardComponent implements OnInit {
  private readonly service = inject(ScreeningService);

  // severity order (left → right on the bar); table already comes most-suspicious first
  private static readonly ORDER = ['likely_novel', 'possible_overlap', 'likely_duplicate'];
  private static readonly COLOR: Record<string, string> = {
    likely_novel: '#0f9d63',
    possible_overlap: '#c77d0a',
    likely_duplicate: '#c0392b',
  };
  private static readonly LABEL: Record<string, string> = {
    likely_novel: 'Novel',
    possible_overlap: 'Possible overlap',
    likely_duplicate: 'Likely duplicate',
  };

  readonly batches = signal<BatchListItem[]>([]);
  readonly selectedId = signal<string>('');
  readonly detail = signal<BatchDetail | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly expanded = signal<Set<string>>(new Set());

  // A full congress is ~10k submissions and ~2.4k flagged rows. Rendering them all in
  // one pass freezes the page and buries the rows that matter, and the table is already
  // sorted most-suspicious first — so show a page at a time.
  private static readonly PAGE = 50;
  readonly shown = signal(ScreeningDashboardComponent.PAGE);

  readonly visibleRows = computed(() => this.detail()?.rows.slice(0, this.shown()) ?? []);
  readonly hasMore = computed(() => (this.detail()?.rows.length ?? 0) > this.shown());
  readonly remaining = computed(() =>
    Math.max(0, (this.detail()?.rows.length ?? 0) - this.shown()),
  );

  /** "before 2026", or null when the batch carried no cutoff. */
  readonly scope = computed(() => {
    const y = this.detail()?.beforeYear;
    return y ? `only abstracts published before ${y}` : null;
  });

  showMore(): void {
    this.shown.update((n) => n + ScreeningDashboardComponent.PAGE);
  }
  showAll(): void {
    this.shown.set(this.detail()?.rows.length ?? ScreeningDashboardComponent.PAGE);
  }

  readonly flagRate = computed(() => {
    const d = this.detail();
    return d && d.total ? Math.round((d.flagged / d.total) * 100) : 0;
  });

  readonly duplicates = computed(() => this.detail()?.verdictCounts?.['likely_duplicate'] ?? 0);

  readonly segments = computed<Segment[]>(() => {
    const d = this.detail();
    if (!d || !d.total) return [];
    return ScreeningDashboardComponent.ORDER.map((v) => {
      const count = d.verdictCounts?.[v] ?? 0;
      return {
        verdict: v,
        label: ScreeningDashboardComponent.LABEL[v],
        color: ScreeningDashboardComponent.COLOR[v],
        count,
        pct: (count / d.total) * 100,
      };
    }).filter((s) => s.count > 0);
  });

  color(verdict: string): string {
    return ScreeningDashboardComponent.COLOR[verdict] ?? '#6b7280';
  }
  label(verdict: string): string {
    return ScreeningDashboardComponent.LABEL[verdict] ?? verdict;
  }

  toggle(id: string): void {
    this.expanded.update((s) => {
      const next = new Set(s);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }
  isExpanded(id: string): boolean {
    return this.expanded().has(id);
  }

  onSelect(id: string): void {
    this.selectedId.set(id);
    this.loadBatch(id);
  }

  private loadBatch(id: string): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.service.getBatch(id).subscribe({
      next: (d) => {
        this.detail.set(d);
        this.shown.set(ScreeningDashboardComponent.PAGE);   // new batch, start at page 1
        this.expanded.set(new Set());
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load batch results.');
        this.loading.set(false);
      },
    });
  }

  ngOnInit(): void {
    this.loading.set(true);
    this.service.listBatches().subscribe({
      next: (list) => {
        this.batches.set(list);
        this.loading.set(false);
        if (list.length > 0) {
          const latest = list[list.length - 1].batchId;
          this.selectedId.set(latest);
          this.loadBatch(latest);
        }
      },
      error: () => {
        this.error.set('Could not load batches.');
        this.loading.set(false);
      },
    });
  }
}
