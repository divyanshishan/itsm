package com.itsm.system.config;

import com.itsm.system.model.entity.Administrator;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.IncidentCategory;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.entity.SlaPolicy;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.enums.BusinessCriticality;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.repository.AdministratorRepository;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.repository.IncidentCategoryRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.repository.SlaPolicyRepository;
import com.itsm.system.repository.TechnicianRepository;
import com.itsm.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Initializes baseline reference data and mock users upon application startup.
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final IncidentCategoryRepository categoryRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final ItServiceRepository itServiceRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final TechnicianRepository technicianRepository;
    private final AdministratorRepository administratorRepository;

    public DataInitializer(IncidentCategoryRepository categoryRepository,
                           SlaPolicyRepository slaPolicyRepository,
                           ItServiceRepository itServiceRepository,
                           UserRepository userRepository,
                           EmployeeRepository employeeRepository,
                           TechnicianRepository technicianRepository,
                           AdministratorRepository administratorRepository) {
        this.categoryRepository = categoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.itServiceRepository = itServiceRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.technicianRepository = technicianRepository;
        this.administratorRepository = administratorRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking initial database state for I-ITSM...");

        Map<String, IncidentCategory> categories = seedIncidentCategories();
        seedSlaPolicies();
        seedItServices(categories);
        seedUsers();

        log.info("I-ITSM Phase 1 Data Initialization completed successfully.");
    }

    private Map<String, IncidentCategory> seedIncidentCategories() {
        Map<String, IncidentCategory> categoryMap = new HashMap<>();

        if (categoryRepository.count() == 0) {
            log.info("Seeding default Incident Categories...");

            createCategory("HARDWARE", "Physical compute, server, workstation, monitor, and peripheral devices",
                    "monitor,screen,laptop,keyboard,mouse,printer,hard drive,ram,cpu,power supply,cable", categoryMap);
            createCategory("SOFTWARE", "Operating system, desktop applications, productivity tools, and utility software",
                    "os,windows,linux,office,excel,word,browser,driver,install,crash,freeze,blue screen", categoryMap);
            createCategory("NETWORK", "Local area network, WiFi, VPN, routers, switches, and internet connectivity",
                    "wifi,vpn,internet,ethernet,router,switch,dns,ip,gateway,latency,packet loss,disconnect", categoryMap);
            createCategory("DATABASE", "Relational and NoSQL database management systems, queries, and data integrity",
                    "sql,postgres,mysql,oracle,mongodb,query,deadlock,database,table,index,corruption", categoryMap);
            createCategory("CLOUD", "Cloud infrastructure, virtual machines, cloud storage buckets, and container services",
                    "aws,azure,gcp,ec2,s3,virtual machine,vm,container,docker,kubernetes,cluster", categoryMap);
            createCategory("SECURITY", "Firewall alerts, malware detection, suspicious activity, unauthorized access attempts",
                    "virus,malware,phishing,ransomware,breach,firewall,unauthorized,hack,suspicious,certificate", categoryMap);
            createCategory("AUTHENTICATION", "User login, Single Sign-On (SSO), LDAP/Active Directory, MFA, password reset",
                    "login,password,sso,mfa,2fa,ldap,active directory,token,credential,locked out,expired", categoryMap);
            createCategory("APPLICATION", "Enterprise business applications, internal CRM, ERP, and payment processing portals",
                    "erp,crm,portal,payment,checkout,api,endpoint,microservice,500,503,timeout", categoryMap);
            createCategory("STORAGE", "Network Attached Storage (NAS), Storage Area Network (SAN), backup volumes, and disks",
                    "nas,san,disk,storage,backup,volume,capacity,quota,full disk,restore", categoryMap);
        } else {
            categoryRepository.findAll().forEach(cat -> categoryMap.put(cat.getName(), cat));
        }

        return categoryMap;
    }

    private void createCategory(String name, String description, String keywords, Map<String, IncidentCategory> map) {
        IncidentCategory category = new IncidentCategory(name, description, keywords, true);
        IncidentCategory saved = categoryRepository.save(category);
        map.put(name, saved);
    }

    private void seedSlaPolicies() {
        if (slaPolicyRepository.count() == 0) {
            log.info("Seeding default SLA Policies...");

            slaPolicyRepository.save(new SlaPolicy(
                    PriorityLevel.P1_CRITICAL,
                    5,
                    30,
                    "P1 Critical Incidents - Severe business stoppage or mission critical outage"
            ));

            slaPolicyRepository.save(new SlaPolicy(
                    PriorityLevel.P2_HIGH,
                    15,
                    120,
                    "P2 High Incidents - High impact with partial degradation or severe workaround"
            ));

            slaPolicyRepository.save(new SlaPolicy(
                    PriorityLevel.P3_MEDIUM,
                    60,
                    480,
                    "P3 Medium Incidents - Moderate impact with acceptable business workaround"
            ));

            slaPolicyRepository.save(new SlaPolicy(
                    PriorityLevel.P4_LOW,
                    240,
                    1440,
                    "P4 Low Incidents - Minor issue, cosmetic defect, or general inquiry"
            ));
        }
    }

    private void seedItServices(Map<String, IncidentCategory> categories) {
        if (itServiceRepository.count() == 0) {
            log.info("Seeding sample IT Services...");

            itServiceRepository.save(new ItService(
                    "SVC-PAY",
                    "Payment Application",
                    "Core online transactions and customer payment processing gateway",
                    BusinessCriticality.MISSION_CRITICAL,
                    categories.get("APPLICATION"),
                    "OPERATIONAL"
            ));

            itServiceRepository.save(new ItService(
                    "SVC-AUTH",
                    "Authentication Service",
                    "Centralized SSO and Identity Provider for all corporate systems",
                    BusinessCriticality.CRITICAL,
                    categories.get("AUTHENTICATION"),
                    "OPERATIONAL"
            ));

            itServiceRepository.save(new ItService(
                    "SVC-NET",
                    "Corporate Network",
                    "Enterprise backbone network, switches, and remote access VPN",
                    BusinessCriticality.HIGH,
                    categories.get("NETWORK"),
                    "OPERATIONAL"
            ));

            itServiceRepository.save(new ItService(
                    "SVC-CLOUD",
                    "Cloud Storage",
                    "Centralized object storage and document repository clusters",
                    BusinessCriticality.MEDIUM,
                    categories.get("STORAGE"),
                    "OPERATIONAL"
            ));

            itServiceRepository.save(new ItService(
                    "SVC-PORTAL",
                    "Internal Portal",
                    "Employee intranet, self-service dashboard, and internal news",
                    BusinessCriticality.LOW,
                    categories.get("APPLICATION"),
                    "OPERATIONAL"
            ));
        }
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding sample Users (Employees, Technicians, Administrator)...");

            // Seed Employees
            Employee e101 = new Employee(
                    "E101",
                    "Alice Johnson",
                    "alice.johnson@itsm.com",
                    "password123",
                    true,
                    "Finance",
                    "Building A, Floor 3"
            );
            employeeRepository.save(e101);

            Employee e102 = new Employee(
                    "E102",
                    "Bob Smith",
                    "bob.smith@itsm.com",
                    "password123",
                    true,
                    "Marketing",
                    "Building B, Floor 1"
            );
            employeeRepository.save(e102);

            Employee e103 = new Employee(
                    "E103",
                    "Priya Patel",
                    "priya.patel@itsm.com",
                    "password123",
                    true,
                    "Operations & HR",
                    "Building C, Floor 2"
            );
            employeeRepository.save(e103);

            // Seed Technicians
            Technician t101 = new Technician(
                    "T101",
                    "Charlie Davis",
                    "charlie.davis@itsm.com",
                    "password123",
                    true,
                    6,
                    true,
                    5
            );
            t101.setSkills(Set.of("NETWORK", "CLOUD", "SECURITY"));
            technicianRepository.save(t101);

            Technician t102 = new Technician(
                    "T102",
                    "Diana Prince",
                    "diana.prince@itsm.com",
                    "password123",
                    true,
                    4,
                    true,
                    5
            );
            t102.setSkills(Set.of("DATABASE", "APPLICATION", "SOFTWARE"));
            technicianRepository.save(t102);

            Technician t103 = new Technician(
                    "T103",
                    "Evan Wright",
                    "evan.wright@itsm.com",
                    "password123",
                    true,
                    3,
                    true,
                    5
            );
            t103.setSkills(Set.of("HARDWARE", "AUTHENTICATION", "STORAGE"));
            technicianRepository.save(t103);

            // Seed Administrator
            Administrator a001 = new Administrator(
                    "A001",
                    "Sarah Connor",
                    "admin@itsm.com",
                    "adminPassword123",
                    true,
                    "SYSTEM_ADMIN"
            );
            administratorRepository.save(a001);
        } else if (employeeRepository.findByUserIdCode("E103").isEmpty()) {
            Employee e103 = new Employee(
                    "E103",
                    "Priya Patel",
                    "priya.patel@itsm.com",
                    "password123",
                    true,
                    "Operations & HR",
                    "Building C, Floor 2"
            );
            employeeRepository.save(e103);
        }
    }
}
