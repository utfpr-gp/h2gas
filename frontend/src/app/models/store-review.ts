// F7 §5.27: GET /suppliers/{id}/reviews — só o primeiro nome de quem avaliou
export interface StoreReview {
  id: number;
  customerFirstName: string;
  rating: number;
  comment: string | null;
  reviewedAt: string | null;
}
