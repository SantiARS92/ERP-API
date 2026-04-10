package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.infrastructure.repository.AppointmentRepository;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.ProfessionalRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.seed.demo-data=true")
@ActiveProfiles("test")
class DevDataSeederIntegrationTest {

    @Autowired
    private DevDataSeeder devDataSeeder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Test
    void shouldSeedDemoDataOnceAndSkipRepeatedExecution() {
        assertThat(categoryRepository.count()).isEqualTo(7);
        assertThat(serviceRepository.count()).isEqualTo(26);
        assertThat(clientRepository.count()).isEqualTo(121);
        assertThat(professionalRepository.count()).isEqualTo(5);
        assertThat(inventoryProductRepository.count()).isEqualTo(48);
        assertThat(expenseRepository.count()).isGreaterThanOrEqualTo(42);
        assertThat(appointmentRepository.count()).isEqualTo(180);
        assertThat(saleRepository.count()).isGreaterThanOrEqualTo(80);

        assertThat(clientRepository.findByFullNameIgnoreCase("Al contado")).isPresent();
        assertThat(professionalRepository.findByActiveOrderByFullNameAsc(true)).hasSizeGreaterThanOrEqualTo(4);
        assertThat(appointmentRepository.findAll().stream().map(appointment -> appointment.getStatus()))
                .contains(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED);
        assertThat(expenseRepository.findAll().stream().map(expense -> expense.getCategory()))
                .contains(ExpenseCategory.SUPPLIES, ExpenseCategory.SERVICES, ExpenseCategory.OTHER);
        assertThat(saleRepository.findAll().stream().map(sale -> sale.getSourceType()))
                .contains(SaleSourceType.MANUAL, SaleSourceType.APPOINTMENT);
        assertThat(inventoryProductRepository.findAll().stream()
                .filter(product -> product.getStock() <= product.getMinimumStock()))
                .isNotEmpty();

        long categoryCount = categoryRepository.count();
        long serviceCount = serviceRepository.count();
        long clientCount = clientRepository.count();
        long professionalCount = professionalRepository.count();
        long inventoryCount = inventoryProductRepository.count();
        long expenseCount = expenseRepository.count();
        long appointmentCount = appointmentRepository.count();
        long saleCount = saleRepository.count();

        devDataSeeder.run(new DefaultApplicationArguments(new String[0]));

        assertThat(categoryRepository.count()).isEqualTo(categoryCount);
        assertThat(serviceRepository.count()).isEqualTo(serviceCount);
        assertThat(clientRepository.count()).isEqualTo(clientCount);
        assertThat(professionalRepository.count()).isEqualTo(professionalCount);
        assertThat(inventoryProductRepository.count()).isEqualTo(inventoryCount);
        assertThat(expenseRepository.count()).isEqualTo(expenseCount);
        assertThat(appointmentRepository.count()).isEqualTo(appointmentCount);
        assertThat(saleRepository.count()).isEqualTo(saleCount);
    }
}
