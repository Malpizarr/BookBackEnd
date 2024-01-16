package org.example.bookfriendship.Repository;

import org.example.bookfriendship.Model.Friendship;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FriendshipRepository extends MongoRepository<Friendship, String> {
    List<Friendship> findByRequesterId(String requesterId);
    List<Friendship> findByFriendId(String friendId);

    List<Friendship> findByRequesterIdAndStatus(String userId, String accepted);

    List<Friendship> findByFriendIdAndStatus(String userId, String accepted);

    boolean existsByRequesterIdAndFriendId(String requesterId, String friendId);

	List<Friendship> findByRequesterIdAndFriendId(String requesterId, String friendId);

	List<Friendship> findByFriendIdAndRequesterId(String friendId, String requesterId);
}
