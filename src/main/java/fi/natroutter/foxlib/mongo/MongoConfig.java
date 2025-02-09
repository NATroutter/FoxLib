package fi.natroutter.foxlib.mongo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class MongoConfig implements Serializable {

    private String uri;
    private String database;

}
