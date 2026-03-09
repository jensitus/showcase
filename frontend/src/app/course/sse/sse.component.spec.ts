import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SseComponent } from './sse.component';

describe('SseComponent', () => {
  let component: SseComponent;
  let fixture: ComponentFixture<SseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SseComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
