package model;

import lombok.*;

/**
 * @author xiaorui
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Album {
    private String artist;
    private String title;
    private String year;
}
