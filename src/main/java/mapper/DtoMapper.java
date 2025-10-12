package mapper;

import DTO.TestimonialDTO;
import DTO.UserDTO;
import model.Testimonial;
import model.User;

public class DtoMapper {

    public static UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId().toString());
        dto.setEmail(user.getEmail());
        dto.setPlan(user.getPlan());
        dto.setCredits(user.getCredits());
        dto.setBrandLogoUrl(user.getBrandLogoUrl());
        dto.setPrimaryColor(user.getPrimaryColor());
        return dto;
    }

    public static TestimonialDTO toDTO(Testimonial t) {
        TestimonialDTO dto = new TestimonialDTO();
        dto.setId(t.getId().toString());
        dto.setUserId(t.getUser().getId().toString());
        dto.setInputText(t.getInputText());
        dto.setSocialPostVersions(t.getSocialPostVersions());
        dto.setHeadlineVersions(t.getHeadlineVersions());
        dto.setShortQuoteVersions(t.getShortQuoteVersions());
        dto.setCallToActionVersions(t.getCallToActionVersions());
        dto.setExportedMd(t.isExportedMd());
        dto.setExportedPng(t.isExportedPng());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }
}