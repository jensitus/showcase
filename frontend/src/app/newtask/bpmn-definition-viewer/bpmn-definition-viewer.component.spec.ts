import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BpmnDefinitionViewerComponent } from './bpmn-definition-viewer.component';

describe('BpmnDefinitionViewerComponent', () => {
  let component: BpmnDefinitionViewerComponent;
  let fixture: ComponentFixture<BpmnDefinitionViewerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BpmnDefinitionViewerComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(BpmnDefinitionViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
