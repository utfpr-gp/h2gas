import { TestBed } from '@angular/core/testing';

import { AbstractService } from './abstract.service';

describe('AbstractService', () => {
  let service: AbstractService<unknown>;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AbstractService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
