export interface UserResponse {
    id: string;
    username: string;
    email: string;
    enabled: boolean;
    role?: string;
    createdAt?: string;
    firstName?: string;
    lastName?: string;
    avatarUrl?: string;
}
