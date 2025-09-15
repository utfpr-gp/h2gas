export interface PaginatedResponse<T> {
    data: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
}
  