package com.acme.todo.rest;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;

import com.acme.todo.domain.TodoEntity;
import com.acme.todo.repository.TodoRepository;

@Path("/todo")
@Produces(MediaType.APPLICATION_JSON)
public class TodoResource {
	private final TodoRepository todoRepository;

	public TodoResource(TodoRepository todoRepository) {
		this.todoRepository = todoRepository;
	}

	@GET
	public List<TodoEntity> findAll() {
		Log.info("Getting all todos");
		return this.todoRepository.listAll(Sort.by("id", Direction.Descending));
	}

	@GET
	@Path("/{id}")
	public TodoEntity findById(@PathParam("id") Long id) {
		Log.infof("Getting todo with id: %d", id);
		return this.todoRepository.findById(id);
	}

	@PUT
	public void update(TodoEntity resource) {
		Log.infof("Updating todo: %s", resource);

		Optional<TodoEntity> entity = QuarkusTransaction.requiringNew()
			.call(() -> this.todoRepository.findByIdOptional(resource.getId())
				.map(todo -> {
					todo.setCompleted(resource.isCompleted());
					todo.setTitle(resource.getTitle());

					return todo;
				})
			);

		// This needs to be done outside the transaction
		entity.filter(TodoEntity::isCompleted)
			.ifPresent(this::completeTodo);
	}

	@POST
	public TodoEntity create(TodoEntity resource) {
		Log.infof("Creating new todo: %s", resource);
		QuarkusTransaction.requiringNew()
			.run(() -> this.todoRepository.persist(resource));

		// This needs to be done outside the transaction
		if (resource.isCompleted()) {
			completeTodo(resource);
		}

		return resource;
	}

	@DELETE
	@Path("/{id}")
	@Transactional
	public void delete(@PathParam("id") Long id) {
		Log.infof("Deleting todo with id: %d: ", id);
		this.todoRepository.deleteById(id);
	}

	private void completeTodo(TodoEntity todo) {
		Log.infof("Completing todo: %s", todo);

		// Extra things we want to do in the future when a Todo is completed
	}
}
