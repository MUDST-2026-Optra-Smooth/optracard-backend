package org.example.dto;

import java.util.List;

public record ProfileResponse(
        Integer userId,
        String username,
        String email,
        String phone,
        String address,
        List<CollectionItemResponse> collection
) {}
