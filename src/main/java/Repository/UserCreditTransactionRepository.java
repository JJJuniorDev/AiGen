package Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.UserCreditTransaction;

public interface UserCreditTransactionRepository extends JpaRepository<UserCreditTransaction, Long>{

}
