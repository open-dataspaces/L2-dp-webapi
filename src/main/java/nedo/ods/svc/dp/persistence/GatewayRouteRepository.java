package nedo.ods.svc.dp.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** JPA Repository for GatewayRoute entities */
@Repository
public interface GatewayRouteRepository extends JpaRepository<GatewayRoute, String> {

  /** Find all enabled routes ordered by order value */
  @Query(
      "SELECT r FROM GatewayRoute r WHERE r.enabled = true ORDER BY COALESCE(r.order, 0) ASC, r.id ASC")
  List<GatewayRoute> findAllEnabledOrderByOrder();

  /** Find routes by enabled status */
  List<GatewayRoute> findByEnabledOrderByOrderAscIdAsc(Boolean enabled);
}
