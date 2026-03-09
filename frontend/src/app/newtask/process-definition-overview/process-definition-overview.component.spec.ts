import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProcessDefinitionOverviewComponent } from './process-definition-overview.component';

describe('ProcessDefinitionOverviewComponent', () => {
  let component: ProcessDefinitionOverviewComponent;
  let fixture: ComponentFixture<ProcessDefinitionOverviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProcessDefinitionOverviewComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ProcessDefinitionOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
