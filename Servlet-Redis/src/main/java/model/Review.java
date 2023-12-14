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
public class Review {
    private int likes;
    private int dislikes;
}
