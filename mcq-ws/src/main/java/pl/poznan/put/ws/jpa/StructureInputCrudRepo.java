package pl.poznan.put.ws.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.poznan.put.ws.entities.StructureInput;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StructureInputCrudRepo extends CrudRepository<StructureInput, UUID> {
  Optional<StructureInput> findByPdbId(String pdbId);

  Optional<StructureInput> findByAssemblyId(int assemblyId);

  Optional<StructureInput> findByStructureContent(String structureContent);
}
