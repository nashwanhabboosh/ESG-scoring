import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MethodologySwitcher } from './methodology-switcher';

describe('MethodologySwitcher', () => {
  let component: MethodologySwitcher;
  let fixture: ComponentFixture<MethodologySwitcher>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MethodologySwitcher],
    }).compileComponents();

    fixture = TestBed.createComponent(MethodologySwitcher);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
