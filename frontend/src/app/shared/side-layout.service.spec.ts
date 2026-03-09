import { TestBed } from '@angular/core/testing';

import { SideLayoutService } from './side-layout.service';

describe('SideLayoutService', () => {
  let service: SideLayoutService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SideLayoutService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
