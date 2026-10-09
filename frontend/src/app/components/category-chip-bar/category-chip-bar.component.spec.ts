import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryChipBarComponent } from './category-chip-bar.component';

describe('CategoryChipBarComponent', () => {
  let component: CategoryChipBarComponent;
  let fixture: ComponentFixture<CategoryChipBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryChipBarComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryChipBarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('deveEmitirNomeDaCategoriaAoClicarNoChip', () => {
    component.categories = [{ id: 1, name: 'Gás' }, { id: 2, name: 'Água' }];
    fixture.detectChanges();
    spyOn(component.categorySelected, 'emit');

    const chipAgua = Array.from(fixture.nativeElement.querySelectorAll('button') as HTMLButtonElement[])
      .find((b) => b.textContent?.includes('Água'));
    chipAgua?.click();

    expect(component.categorySelected.emit).toHaveBeenCalledWith('Água');
  });

  it('deveEmitirNullAoClicarEmTodas', () => {
    component.categories = [{ id: 1, name: 'Gás' }];
    fixture.detectChanges();
    spyOn(component.categorySelected, 'emit');

    const chipTodas = Array.from(fixture.nativeElement.querySelectorAll('button') as HTMLButtonElement[])
      .find((b) => b.textContent?.includes('Todas'));
    chipTodas?.click();

    expect(component.categorySelected.emit).toHaveBeenCalledWith(null);
  });
});
