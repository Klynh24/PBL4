package pbl.backend.kchi.validator;


import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pbl.backend.kchi.annotations.UniqueEmail;
import pbl.backend.kchi.modules.users.repositories.UserRepository;


public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {


    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context){
        return !userRepository.existsByEmail(email);
    }

}
