package main;

import java.util.Set;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.Enrollment;

public class ObcsUser implements User {
        public String name;
        public String mspId;
        public Enrollment enrollment;

        public String getEnrollmentSecret() {
            return enrollmentSecret;
        }

        public String enrollmentSecret;

        public ObcsUser(String name, String mspId) {
            this.name = name;
            this.mspId = mspId;
        }

        public void setEnrollment(Enrollment e) {
            this.enrollment = e;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Set<String> getRoles() {
            return null;
        }

        @Override
        public String getAccount() {
            return null;
        }

        @Override
        public String getAffiliation() {
            return null;
        }

        @Override
        public Enrollment getEnrollment() {
            return enrollment;
        }

        @Override
        public String getMspId() {
            return mspId;
        }

        public void setEnrollmentSecret(String enrollmentSecret) {

            this.enrollmentSecret = enrollmentSecret;
        }
    }