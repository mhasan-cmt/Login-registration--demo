package com.devmahmud.registrationDemo.appuser;

import com.devmahmud.registrationDemo.registration.token.ConfirmationToken;
import com.devmahmud.registrationDemo.registration.token.ConfirmationTokenService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AppUserService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ConfirmationTokenService tokenService;

    private final static String USER_NOT_FOUND_MSG =
            "User with email %s not found!";
    private final static String USER_ALREADY_EXISTS_MSG =
            "User with email %s already exists!";

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        return userRepository.findByEmail(email).orElseThrow(() ->
                new UsernameNotFoundException(String.format(
                USER_NOT_FOUND_MSG, email
        )));
    }

    public String signUp(AppUser user){
        boolean userExists = userRepository
                .findByEmail(user.getEmail())
                .isPresent();

        if(userExists){
            //TODO:check user confirmed, else send a new token
            ConfirmationToken confirmationToken = tokenService.getTokenByUserName(user.getEmail()).orElseThrow(()-> new IllegalStateException("No Token found"));
            if(!confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())){
                return "You have wait to  more: "+ ChronoUnit.MINUTES.between(confirmationToken.getExpiresAt(),LocalDateTime.now()) +" minutes to generate a new token";
            }
            if (confirmationToken.getConfirmedAt()==null){
                String newToken = UUID.randomUUID().toString();
                confirmationToken.setToken(newToken);
                tokenService.saveConfirmationToken(confirmationToken);
                return "new token generated: "+newToken;
            }
            throw new IllegalStateException(
                    String.format(USER_ALREADY_EXISTS_MSG, user.getEmail())
            );
        }
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);

        //TODO: Send confirmation generatedToken
        String generatedToken = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(
                generatedToken,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15),
                user
        );
        tokenService.saveConfirmationToken(confirmationToken);
        //TODO: SEND EMAIL WITH TOKEN
        return generatedToken;
    }

    public int enableAppUser(String email){
        return userRepository.enableAppUser(email);
    }
}
