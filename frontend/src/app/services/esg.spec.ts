import { TestBed } from '@angular/core/testing';

import { Esg } from './esg';

describe('Esg', () => {
  let service: Esg;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Esg);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
